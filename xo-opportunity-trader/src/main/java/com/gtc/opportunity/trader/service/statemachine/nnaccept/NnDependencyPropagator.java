package com.gtc.opportunity.trader.service.statemachine.nnaccept;

import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.gtc.opportunity.trader.domain.TradeEvent.DEPENDENCY_DONE;

/**
 * Created by Valentyn Berezin on 08.08.18.
 */
@Service
@RequiredArgsConstructor
public class NnDependencyPropagator {

    private final StateMachineService<TradeStatus, TradeEvent> tradeMachineService;

    @Transactional
    public void ackDependencyDone(String id) {
        StateMachine<TradeStatus, TradeEvent> machine = tradeMachineService.acquireStateMachine(id);
        machine.sendEvent(DEPENDENCY_DONE);
        tradeMachineService.releaseStateMachine(id);
    }
}
