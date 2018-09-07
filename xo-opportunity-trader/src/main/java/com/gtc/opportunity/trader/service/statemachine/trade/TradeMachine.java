package com.gtc.opportunity.trader.service.statemachine.trade;

import com.gtc.opportunity.trader.domain.AcceptEvent;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Using proxies because @OnTransition annotation requires hackish setting of BeanName to StateMachineFactory +
 * incorrectly handles annotations - Transactional annotation causes enum-based custom annotation to fire twice.
 */
@Slf4j
@Service
@Retryable(value = TransientDataAccessException.class,
        maxAttemptsExpression = "3",
        backoff = @Backoff(delay = 5000L, multiplier = 3),
        exceptionExpression = "#{#root.cause instanceof T(org.hibernate.exception.LockAcquisitionException)}"
)
@RequiredArgsConstructor
public class TradeMachine {

    private final SoftCancelHandler softCancelHandler;
    private final BalanceReleaser releaser;
    private final TradeMessageProcessor processor;
    private final AcceptInteractor acceptInteractor;

    @Transactional
    public void doneDependency(StateContext<TradeStatus, TradeEvent> state) {
        processor.acceptAndGet(state, (trade, value) -> {});
    }

    @Transactional
    public void ack(StateContext<TradeStatus, TradeEvent> state) {
        processor.acceptAndGet(state, (trade, value) -> {});
        acceptInteractor.sendToSuperIfExists(id(state), state, AcceptEvent.TRADE_ACK);
        releaser.release(id(state));
    }

    @Transactional
    public void done(StateContext<TradeStatus, TradeEvent> state) {
        processor.acceptAndGet(state, (trade, value) -> {}).ifPresent(trade -> updateSoftCancelIfNeeded(trade, state));
        acceptInteractor.sendToSuperIfExists(id(state), state, AcceptEvent.TRADE_DONE);
        releaser.release(id(state));
    }

    @Transactional
    public void cancel(StateContext<TradeStatus, TradeEvent> state) {
        processor.acceptAndGet(state, (trade, value) -> {}).ifPresent(trade -> updateSoftCancelIfNeeded(trade, state));
        acceptInteractor.sendToSuperIfExists(id(state), state, AcceptEvent.CANCEL);
        releaser.release(id(state), true);
    }

    @Transactional
    public void softCancel(StateContext<TradeStatus, TradeEvent> state) {
        processor.acceptAndGet(state, (trade, value) -> {}).ifPresent(trade -> updateSoftCancelIfNeeded(trade, state));
        // Not sending to super here, because we should be able to rollback safely
        releaser.release(id(state), true);
    }

    @Transactional
    public void transientError(StateContext<TradeStatus, TradeEvent> state) {
        error(state);
    }

    @Transactional
    public void fatalError(StateContext<TradeStatus, TradeEvent> state) {
        error(state);
    }

    @Transactional
    public void timeout(StateContext<TradeStatus, TradeEvent> state) {
        error(state);
    }

    @Transactional
    public void retry(StateContext<TradeStatus, TradeEvent> state) {
        //FIXME NOOP
    }

    private void error(StateContext<TradeStatus, TradeEvent> state) {
        processor.acceptAndGet(state, Trade::setLastError).ifPresent(trade -> updateSoftCancelIfNeeded(trade, state));
        acceptInteractor.sendToSuperIfExists(id(state), state, AcceptEvent.ISSUE);
        releaser.release(id(state), true);
    }

    private void updateSoftCancelIfNeeded(Trade trade, StateContext<TradeStatus, TradeEvent> state) {
        softCancelHandler.updateSoftCancelIfNeeded(trade, state.getTarget().getId());
    }

    private static String id(StateContext<TradeStatus, TradeEvent> state) {
        return state.getStateMachine().getId();
    }
}
