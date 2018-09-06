package com.gtc.opportunity.trader.service.scheduled.trade.management;

import com.gtc.model.gateway.command.manage.CancelOrderCommand;
import com.gtc.opportunity.trader.domain.*;
import com.gtc.opportunity.trader.repository.SoftCancelConfigRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import com.gtc.opportunity.trader.service.TradeCreationService;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.gtc.opportunity.trader.service.dto.TradeDto;
import com.gtc.opportunity.trader.service.xoopportunity.creation.ConfigCache;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.RejectionException;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 03.09.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NnOrderSoftCanceller {

    private final WsGatewayCommander commander;
    private final CurrentTimestamp dbTime;
    private final SoftCancelConfigRepository softCancelConfig;
    private final StateMachineService<TradeStatus, TradeEvent> tradeMachines;
    private final OldOrderFinder finder;
    private final TradeCreationService tradesService;
    private final ConfigCache cache;
    private final NnOrderCancelPriceFinder cancelPriceFinder;

    @Trace(dispatcher = true)
    @Transactional(readOnly = true)
    @Scheduled(fixedDelayString = "#{${app.schedule.order.cancellerS} * 1000}")
    public void softCancel() {
        LocalDateTime now = dbTime.dbNow();
        Map<OldOrderFinder.Key, SoftCancelConfig> byClient = getActiveConfigs();

        finder.expiredSlave(
                now,
                byClient.entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, it -> it.getValue().getWaitM())
                )
        ).forEach(it -> cancelAndReplaceOrderIfPossible(byClient, it));
    }

    private void cancelAndReplaceOrderIfPossible(Map<OldOrderFinder.Key, SoftCancelConfig> cancels, Trade trade) {
        Optional<ClientConfig> config = cache.getClientCfg(
                trade.getClient().getName(), trade.getCurrencyFrom(), trade.getCurrencyTo());
        SoftCancelConfig cancelCfg = cancels.get(
                new OldOrderFinder.Key(trade.getClient().getName(), trade.getCurrencyFrom(), trade.getCurrencyTo())
        );

        if (null == cancelCfg || !config.isPresent()) {
            return;
        }

        log.info("Attempting to soft-cancel order {}", trade.getId());
        BigDecimal lossPrice = cancelPriceFinder.findSuitableLossPrice(cancelCfg, trade);
        if (null == lossPrice) {
            return;
        }

        TradeDto softCancel = createSoftCancelTrade(config.get(), lossPrice, trade);
        if (null == softCancel) {
            return;
        }

        StateMachine<TradeStatus, TradeEvent> machine = tradeMachines.acquireStateMachine(trade.getId());
        machine.sendEvent(TradeEvent.SOFT_CANCELLED);
        tradeMachines.releaseStateMachine(machine.getId());

        log.info("Cancelling order {} and replacing it", trade.getId());
        commander.cancel(new CancelOrderCommand(
                trade.getClient().getName(),
                trade.getId(),
                trade.getAssignedId()
        ));

        log.info("Pushing soft-cancel order {}", softCancel.getCommand());
        commander.createOrder(softCancel.getCommand());

        // TODO: acknowledge top-order machine that it is done in soft-cancel mode, consider transaction nesting
    }

    private TradeDto createSoftCancelTrade(ClientConfig config, BigDecimal price, Trade trade) {
        try {
            TradeDto cancel = tradesService.createTradeNoSideValidation(
                    trade.getDependsOn(),
                    config,
                    price,
                    trade.getAmount().abs(), // caution here - we can't use opening amount
                    trade.isSell(),
                    true
            );

            StateMachine<TradeStatus, TradeEvent> machine = tradeMachines
                    .acquireStateMachine(cancel.getTrade().getId());
            machine.sendEvent(TradeEvent.DEPENDENCY_DONE);
            tradeMachines.releaseStateMachine(machine.getId());

            return cancel;
        } catch (RejectionException ex) {
            log.warn("Soft-cancel trade could not be created, aborting", ex);
            return null;
        }
    }

    private Map<OldOrderFinder.Key, SoftCancelConfig> getActiveConfigs() {
        return softCancelConfig.findAllActive().stream()
                .collect(Collectors.toMap(
                        it -> new OldOrderFinder.Key(
                                it.getClientCfg().getClient().getName(),
                                it.getClientCfg().getCurrency(),
                                it.getClientCfg().getCurrencyTo()),
                        it -> it)
                );
    }
}
