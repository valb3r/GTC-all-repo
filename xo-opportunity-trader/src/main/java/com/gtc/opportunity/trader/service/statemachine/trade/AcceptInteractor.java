package com.gtc.opportunity.trader.service.statemachine.trade;

import com.gtc.opportunity.trader.domain.*;
import com.gtc.opportunity.trader.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.gtc.opportunity.trader.config.Const.Common.NN_OPPORTUNITY_PREFIX;
import static com.gtc.opportunity.trader.config.Const.Common.XO_OPPORTUNITY_PREFIX;
import static com.gtc.opportunity.trader.domain.Const.InternalMessaging.MSG_ID;
import static com.gtc.opportunity.trader.domain.Const.InternalMessaging.ORDER_ID;
import static com.gtc.opportunity.trader.domain.Const.InternalMessaging.TRADE_ID;

/**
 * Created by Valentyn Berezin on 03.04.18.
 */
@Slf4j
@Service
@Retryable(value = TransientDataAccessException.class,
        maxAttemptsExpression = "3",
        backoff = @Backoff(delay = 5000L, multiplier = 3),
        exceptionExpression = "#{#root.cause instanceof T(org.hibernate.exception.LockAcquisitionException)}"
)
@RequiredArgsConstructor
public class AcceptInteractor {

    private final TradeRepository tradeRepository;
    private final StateMachineService<XoAcceptStatus, AcceptEvent> xoMachineSvc;
    private final StateMachineService<NnAcceptStatus, AcceptEvent> nnMachineSvc;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendToXoIfExists(String tradeId, StateContext<TradeStatus, TradeEvent> state, AcceptEvent status) {
        log.info("Attempt to interact with higher level machine for trade {} / {} / {}", tradeId, state, status);
        tradeRepository.findById(tradeId).ifPresent(trade -> {
            propagateToXoIfNeeded(trade, state, status);
            propagateToNnIfNeeded(trade, state, status);
        });
    }

    private void propagateToXoIfNeeded(Trade trade, StateContext<TradeStatus, TradeEvent> state, AcceptEvent status) {
        if (null == trade.getXoOrder()) {
            return;
        }

        int orderId = trade.getXoOrder().getId();
        String machineId = XO_OPPORTUNITY_PREFIX + orderId;
        Message<AcceptEvent> msg = builder(trade, orderId, state, status).build();
        log.info("Sending request to {} / {}", machineId, msg);
        xoMachineSvc.acquireStateMachine(machineId).sendEvent(msg);
        xoMachineSvc.releaseStateMachine(machineId);
    }

    private void propagateToNnIfNeeded(Trade trade, StateContext<TradeStatus, TradeEvent> state, AcceptEvent status) {
        if (null == trade.getNnOrder()) {
            return;
        }

        int orderId = trade.getNnOrder().getId();
        String machineId = NN_OPPORTUNITY_PREFIX + orderId;
        Message<AcceptEvent> msg = builder(trade, orderId, state, status).build();
        log.info("Sending request to {} / {}", machineId, msg);
        nnMachineSvc.acquireStateMachine(machineId).sendEvent(msg);
        nnMachineSvc.releaseStateMachine(machineId);
    }

    private <E, S, T> MessageBuilder<E> builder(Trade trade, int orderId, StateContext<S, T> state, E status) {
        return MessageBuilder
                .withPayload(status)
                .setHeader(ORDER_ID, orderId)
                .setHeader(TRADE_ID, trade.getId())
                .setHeader(MSG_ID, state.getMessageHeaders().get(TradeEvent.MSG_ID));
    }
}
