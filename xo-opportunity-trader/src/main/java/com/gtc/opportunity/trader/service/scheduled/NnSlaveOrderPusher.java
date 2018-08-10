package com.gtc.opportunity.trader.service.scheduled;

import com.gtc.opportunity.trader.domain.AcceptEvent;
import com.gtc.opportunity.trader.domain.NnAcceptStatus;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.statemachine.nnaccept.NnDependencyHandler;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static com.gtc.opportunity.trader.config.Const.Common.NN_OPPORTUNITY_PREFIX;
import static com.gtc.opportunity.trader.domain.Const.InternalMessaging.ORDER_ID;

/**
 * Uses lightweight retry-alike logic to wait for balance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NnSlaveOrderPusher {

    private final NnDependencyHandler handler;
    private final TradeRepository repository;
    private final StateMachineService<NnAcceptStatus, AcceptEvent> nnMachineSvc;

    @Trace(dispatcher = true) // tracing since it is very important part
    @Transactional
    @Scheduled(fixedDelayString = "#{${app.schedule.pushSlaveS} * 1000}")
    public void pushOrders() {
        repository.findDependantsByMasterStatus(
                Collections.singleton(TradeStatus.DEPENDS_ON),
                Collections.singleton(TradeStatus.CLOSED)
        ).forEach(this::ackAndCreateOrders);
    }

    private void ackAndCreateOrders(Trade trade) {
        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName("Push dependent " + trade.getId());
        try {
            if (!handler.publishDependentOrderIfPossible(trade)) {
                return;
            }

            String machineId = NN_OPPORTUNITY_PREFIX + trade.getNnOrder().getId();
            nnMachineSvc.acquireStateMachine(machineId).sendEvent(MessageBuilder
                    .withPayload(AcceptEvent.CONTINUE)
                    .setHeader(ORDER_ID, trade.getNnOrder().getId())
                    .build()
            );
            nnMachineSvc.releaseStateMachine(machineId);
        } catch (RuntimeException ex) {
            log.warn("Exception trying propagate order id {}", trade.getId(), ex);
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }
}
