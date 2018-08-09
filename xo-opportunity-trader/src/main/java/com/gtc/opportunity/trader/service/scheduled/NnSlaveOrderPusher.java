package com.gtc.opportunity.trader.service.scheduled;

import com.gtc.opportunity.trader.domain.AcceptEvent;
import com.gtc.opportunity.trader.domain.NnAcceptStatus;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static com.gtc.opportunity.trader.config.Const.Common.NN_OPPORTUNITY_PREFIX;
import static com.gtc.opportunity.trader.domain.Const.InternalMessaging.ORDER_ID;
import static com.gtc.opportunity.trader.domain.Const.InternalMessaging.TRADE_ID;

/**
 * Uses lightweight retry-alike logic to wait for balance.
 */
@Service
@RequiredArgsConstructor
public class NnSlaveOrderPusher {

    private final TradeRepository repository;
    private final StateMachineService<NnAcceptStatus, AcceptEvent> nnMachineSvc;

    @Transactional
    @Scheduled(fixedDelayString = "#{${app.schedule.pushSlaveS} * 1000}")
    public void pushOrders() {
        repository.findDependantsByMasterStatus(
                Collections.singleton(TradeStatus.DEPENDS_ON),
                Collections.singleton(TradeStatus.CLOSED)
        ).forEach(trade -> {
            String machineId = NN_OPPORTUNITY_PREFIX + trade.getNnOrder().getId();
            nnMachineSvc.acquireStateMachine(machineId).sendEvent(
                    MessageBuilder
                            .withPayload(AcceptEvent.CONTINUE)
                            .setHeader(ORDER_ID, trade.getNnOrder().getId())
                            .setHeader(TRADE_ID, trade.getDependsOn().getId())
                    .build()
            );
            nnMachineSvc.releaseStateMachine(machineId);
        });
    }
}
