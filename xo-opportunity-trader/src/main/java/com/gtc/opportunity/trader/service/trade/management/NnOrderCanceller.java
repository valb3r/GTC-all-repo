package com.gtc.opportunity.trader.service.trade.management;

import com.google.common.collect.ImmutableList;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.gateway.command.manage.CancelOrderCommand;
import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.repository.NnConfigRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.newrelic.api.agent.Trace;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Trace(dispatcher = true)
    @Transactional(readOnly = true)
    @Scheduled(fixedDelayString = "#{${app.schedule.order.cancellerS} * 1000}")
    public void cancel() {
        LocalDateTime now = dbTime.dbNow();
        Map<Key, NnConfig> byClient = getActiveConfigs();

        OptionalInt minHours = byClient.values().stream().mapToInt(NnConfig::getExpireOpenH).min();

        if (!minHours.isPresent()) {
            return;
        }

        LocalDateTime atLeastOld = now.minusHours(minHours.getAsInt());
        List<Trade> expired = tradeRepository
                .findNnByStatusInAndStatusUpdatedBefore(ImmutableList.of(OPENED), atLeastOld, true);

        List<Trade> toCancel = new ArrayList<>();
        for (Trade trade : expired) {
            Key key = new Key(
                    trade.getClient().getName(),
                    trade.getCurrencyFrom(),
                    trade.getCurrencyTo()
            );

            NnConfig config = byClient.get(key);
            if (null == config || now.minusHours(config.getExpireOpenH()).compareTo(trade.getStatusUpdated()) < 0) {
                continue;
            }
            toCancel.add(trade);
        }

        log.info("Cancelling {} orders", toCancel.size());
        toCancel.forEach(it -> commander.cancel(new CancelOrderCommand(
                it.getClient().getName(),
                it.getId(),
                it.getAssignedId()
        )));
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

    @Data
    private static class Key {

        private final String clientName;
        private final TradingCurrency from;
        private final TradingCurrency to;
    }
}
