package com.gtc.opportunity.trader.service.scheduled.trade.management;

import com.google.common.collect.ImmutableList;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.gateway.command.manage.CancelOrderCommand;
import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.NnConfigRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.newrelic.api.agent.Trace;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static com.gtc.opportunity.trader.domain.TradeStatus.OPENED;

/**
 * Created by Valentyn Berezin on 07.08.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NnOrderCanceller {

    private final WsGatewayCommander commander;
    private final TradeRepository tradeRepository;
    private final CurrentTimestamp dbTime;
    private final NnConfigRepository nnCfg;
    private final StateMachineService<TradeStatus, TradeEvent> tradeMachines;

    @Trace(dispatcher = true)
    @Transactional(readOnly = true)
    @Scheduled(fixedDelayString = "#{${app.schedule.order.cancellerS} * 1000}")
    public void cancel() {
        LocalDateTime now = dbTime.dbNow();
        Map<Key, NnConfig> byClient = getActiveConfigs();

        cancelOldMasters(now, byClient);
        cancelOldSlaves(now, byClient);
    }

    private void cancelOldMasters(LocalDateTime now, Map<Key, NnConfig> active) {
        log.info("Cancelling old masters");
        OptionalInt minMinutes = active.values().stream().mapToInt(NnConfig::getMaxSlaveDelayM).min();

        if (!minMinutes.isPresent()) {
            return;
        }

        LocalDateTime atLeastOld = now.minusMinutes(minMinutes.getAsInt());
        List<Trade> expired = tradeRepository
                .findNnMasterByStatusInAndRecordedOnBefore(ImmutableList.of(OPENED), atLeastOld, true);

        for (Trade trade : expired) {
            cancelMaster(active.get(key(trade)), now, trade);
        }
    }

    private void cancelOldSlaves(LocalDateTime now, Map<Key, NnConfig> active) {
        log.info("Cancelling old slave");
        OptionalInt minHours = active.values().stream().mapToInt(NnConfig::getExpireOpenH).min();

        if (!minHours.isPresent()) {
            return;
        }

        LocalDateTime atLeastOld = now.minusHours(minHours.getAsInt());
        List<Trade> expired = tradeRepository
                .findNnSlaveByStatusInAndStatusUpdatedBefore(ImmutableList.of(OPENED), atLeastOld, true);

        for (Trade trade : expired) {
            cancelSlave(active.get(key(trade)), now, trade);
        }
    }

    private void cancelSlave(NnConfig config, LocalDateTime now, Trade trade) {
        if (null == config || now.minusHours(config.getExpireOpenH()).compareTo(trade.getStatusUpdated()) < 0) {
            return;
        }

        log.info("Trade {} is opened for too long", trade.getId());
        cancelOrder(trade);
    }

    private void cancelMaster(NnConfig config, LocalDateTime now, Trade trade) {

        if (null == config || trade.getRecordedOn().plusMinutes(config.getMaxSlaveDelayM()).compareTo(now) > 0) {
            return;
        }

        log.info("Trade can't propagate slave timely - sending cancel signal", trade.getId());
        cancelOrder(trade);
    }

    private void cancelOrder(Trade trade) {
        log.info("Cancelling order {}", trade.getId());
        commander.cancel(new CancelOrderCommand(
                trade.getClient().getName(),
                trade.getId(),
                trade.getAssignedId()
        ));

        StateMachine<TradeStatus, TradeEvent> machine = tradeMachines.acquireStateMachine(trade.getId());
        machine.sendEvent(TradeEvent.CANCELLED);
        tradeMachines.releaseStateMachine(machine.getId());
    }

    private Map<Key, NnConfig> getActiveConfigs() {
        return nnCfg.findAllActive().stream()
                .collect(Collectors.toMap(
                        it -> new Key(
                                it.getClientCfg().getClient().getName(),
                                it.getClientCfg().getCurrency(),
                                it.getClientCfg().getCurrencyTo()),
                        it -> it)
                );
    }

    private static Key key(Trade trade) {
        return new Key(
                trade.getClient().getName(),
                trade.getCurrencyFrom(),
                trade.getCurrencyTo()
        );
    }

    @Data
    private static class Key {

        private final String clientName;
        private final TradingCurrency from;
        private final TradingCurrency to;
    }
}
