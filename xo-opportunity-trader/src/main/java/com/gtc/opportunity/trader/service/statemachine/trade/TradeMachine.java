package com.gtc.opportunity.trader.service.statemachine.trade;

import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.domain.XoAcceptEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Using proxies because @OnTransition annotation requires hackish setting of BeanName to StateMachineFactory +
 * incorrectly handles annotations - Transactional annotation causes enum-based custom annotation to fire twice.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeMachine {

    private final TradeMessageProcessor processor;
    private final XoInteractor xoInteractor;

    @Transactional
    public void ack(StateContext<TradeStatus, TradeEvent> state) {
        processor.acceptAndGet(state, (trade, value) -> {});
        xoInteractor.sendToXoIfExists(state.getStateMachine().getId(), state, XoAcceptEvent.TRADE_ACK);
    }

    @Transactional
    public void cancel(StateContext<TradeStatus, TradeEvent> state) {
        processor.acceptAndGet(state, (trade, value) -> {});
        // FIXME NOP ?
    }

    @Transactional
    public void done(StateContext<TradeStatus, TradeEvent> state) {
        processor.acceptAndGet(state, (trade, value) -> {});
        xoInteractor.sendToXoIfExists(state.getStateMachine().getId(), state, XoAcceptEvent.TRADE_DONE);
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
        processor.acceptAndGet(state, Trade::setLastError);
        xoInteractor.sendToXoIfExists(state.getStateMachine().getId(), state, XoAcceptEvent.TRADE_ISSUE);
    }
}
