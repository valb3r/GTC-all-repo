package com.gtc.opportunity.trader.service.statemachine.xoaccept;

import com.gtc.opportunity.trader.domain.AcceptedXoTrade;
import com.gtc.opportunity.trader.domain.AcceptEvent;
import com.gtc.opportunity.trader.domain.XoAcceptStatus;
import com.gtc.opportunity.trader.domain.XoConfig;
import com.gtc.opportunity.trader.repository.AcceptedXoTradeRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.xoopportunity.creation.ConfigCache;
import com.gtc.opportunity.trader.service.xoopportunity.replenishment.TradeReplenishmentService;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gtc.opportunity.trader.domain.Const.InternalMessaging.MSG_ID;
import static com.gtc.opportunity.trader.domain.Const.InternalMessaging.ORDER_ID;

/**
 * Created by Valentyn Berezin on 02.03.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XoAcceptMachine {

    private final ConfigCache cfgCache;
    private final TradeRepository tradeRepository;
    private final AcceptedXoTradeRepository xoTradeRepository;
    private final TradeReplenishmentService replenishmentService;

    @Transactional
    public void ack(StateContext<XoAcceptStatus, AcceptEvent> state) {
        log.info("Confirm evt {}", state);
        acceptAndGet(state);
    }

    @Transactional
    public void done(StateContext<XoAcceptStatus, AcceptEvent> state) {
        log.info("Trade done evt {}", state);
        acceptAndGet(state);
    }

    @Transactional
    public void tradeComplete(StateContext<XoAcceptStatus, AcceptEvent> state) {
        log.info("Trade complete evt {}", state);
        acceptAndGet(state);
    }

    @Transactional
    public void replenishmentComplete(StateContext<XoAcceptStatus, AcceptEvent> state) {
        log.info("Replenisment done evt {}", state);
        acceptAndGet(state);
    }

    @Transactional
    public void tradeError(StateContext<XoAcceptStatus, AcceptEvent> state) {
        log.info("Trade Error evt {}", state);
        acceptAndGet(state);
    }

    @Async
    @Trace(dispatcher = true)
    @Transactional
    public void replenish(StateContext<XoAcceptStatus, AcceptEvent> state) {
        log.info("Replenish evt {}", state);
        acceptAndGet(state).ifPresent(replenishmentService::replenish);
    }

    @Transactional
    public void complete(StateContext<XoAcceptStatus, AcceptEvent> state) {
        log.info("Complete evt {}", state);
        acceptAndGet(state, XoAcceptStatus.DONE);
    }

    @Transactional
    public boolean canReplenish(StateContext<XoAcceptStatus, AcceptEvent> state) {
        return acceptAndGet(state).map(xoTrade -> {
            List<Boolean> enabled = tradeRepository.findByXoOrderId(xoTrade.getId()).stream()
                    .map(it -> cfgCache
                            .getXoCfg(it.getClient().getName(), it.getCurrencyFrom(), it.getCurrencyTo())
                    ).flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                    .map(XoConfig::isReplenishable)
                    .collect(Collectors.toList());
            // currently we replenish both or none
            return !enabled.contains(false) && 2 == enabled.size();
        }).orElse(false);
    }

    @Transactional
    public void error(StateContext<XoAcceptStatus, AcceptEvent> state) {
        log.info("Error evt {}", state);
        acceptAndGet(state, XoAcceptStatus.ERROR);
        state.getStateMachine().setStateMachineError(state.getException());
    }

    private Optional<AcceptedXoTrade> acceptAndGet(StateContext<XoAcceptStatus, AcceptEvent> state) {
        return acceptAndGet(state, null);
    }

    private Optional<AcceptedXoTrade> acceptAndGet(StateContext<XoAcceptStatus, AcceptEvent> state,
                                                   XoAcceptStatus status) {
        int id = (int) state.getMessage().getHeaders().get(ORDER_ID);

        return xoTradeRepository.findById(id).map(trade -> {
            trade.setLastMessageId((String) state.getMessage().getHeaders().get(MSG_ID));
            trade.setStatus(null != status ? status : state.getTarget().getId());

            return xoTradeRepository.save(trade);
        });
    }
}
