package com.gtc.opportunity.trader.service.scheduled.trade.management;

import com.gtc.model.gateway.command.manage.CancelOrderCommand;
import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.NnConfigRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 07.08.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NnOrderHardCanceller {

    private final WsGatewayCommander commander;
    private final CurrentTimestamp dbTime;
    private final NnConfigRepository nnCfg;
    private final StateMachineService<TradeStatus, TradeEvent> tradeMachines;
    private final OldOrderFinder oldOrderFinder;

    @Trace(dispatcher = true)
    @Transactional(readOnly = true)
    @Scheduled(fixedDelayString = "#{${app.schedule.order.cancellerS} * 1000}")
    public void hardCancel() {
        LocalDateTime now = dbTime.dbNow();
        Map<OldOrderFinder.Key, NnConfig> byClient = getActiveConfigs();

        oldOrderFinder.expiredMaster(now, byClient.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, it -> it.getValue().getMaxSlaveDelayM())))
                .forEach(this::cancelOrder);
        oldOrderFinder.expiredSlave(now, byClient.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, it -> it.getValue().getExpireOpenH() * 60)))
                .forEach(this::cancelOrder);
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

    private Map<OldOrderFinder.Key, NnConfig> getActiveConfigs() {
        return nnCfg.findAllActive().stream()
                .collect(Collectors.toMap(
                        it -> new OldOrderFinder.Key(
                                it.getClientCfg().getClient().getName(),
                                it.getClientCfg().getCurrency(),
                                it.getClientCfg().getCurrencyTo()),
                        it -> it)
                );
    }
}
