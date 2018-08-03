package com.gtc.opportunity.trader.service.statemachine.nnaccept;

import com.gtc.opportunity.trader.domain.AcceptedNnTrade;
import com.gtc.opportunity.trader.domain.AcceptEvent;
import com.gtc.opportunity.trader.domain.NnAcceptStatus;
import com.gtc.opportunity.trader.repository.AcceptedNnTradeRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.gtc.opportunity.trader.domain.Const.InternalMessaging.MSG_ID;
import static com.gtc.opportunity.trader.domain.Const.InternalMessaging.ORDER_ID;

/**
 * Created by Valentyn Berezin on 02.03.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NnAcceptMachine {

    private final CurrentTimestamp timestamp;
    private final AcceptedNnTradeRepository nnTradeRepository;

    @Transactional
    public void ack(StateContext<NnAcceptStatus, AcceptEvent> state) {
        log.info("Confirm evt {}", state);
        acceptAndGet(state);
    }

    @Transactional
    public void done(StateContext<NnAcceptStatus, AcceptEvent> state) {
        log.info("Trade done evt {}", state);
        acceptAndGet(state);
    }

    @Transactional
    public void tradeError(StateContext<NnAcceptStatus, AcceptEvent> state) {
        log.info("Trade Error evt {}", state);
        acceptAndGet(state);
    }

    @Transactional
    public void error(StateContext<NnAcceptStatus, AcceptEvent> state) {
        log.info("Error evt {}", state);
        acceptAndGet(state, NnAcceptStatus.ERROR);
        state.getStateMachine().setStateMachineError(state.getException());
    }

    private Optional<AcceptedNnTrade> acceptAndGet(StateContext<NnAcceptStatus, AcceptEvent> state) {
        return acceptAndGet(state, null);
    }

    private Optional<AcceptedNnTrade> acceptAndGet(StateContext<NnAcceptStatus, AcceptEvent> state,
                                                   NnAcceptStatus status) {
        int id = (int) state.getMessage().getHeaders().get(ORDER_ID);

        return nnTradeRepository.findById(id).map(trade -> {
            trade.setLastMessageId((String) state.getMessage().getHeaders().get(MSG_ID));
            trade.setStatus(null != status ? status : state.getTarget().getId());

            if (NnAcceptStatus.DONE == trade.getStatus()) {
                trade.setClosedOn(timestamp.dbNow());
            }

            return nnTradeRepository.save(trade);
        });
    }
}
