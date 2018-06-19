package com.gtc.opportunity.trader.service.statemachine.trade;

import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.domain.XoAcceptEvent;
import com.gtc.opportunity.trader.domain.XoAcceptStatus;
import com.gtc.opportunity.trader.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.gtc.opportunity.trader.config.Const.Common.XO_OPPORTUNITY_PREFIX;

/**
 * Created by Valentyn Berezin on 03.04.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XoInteractor {

    private final TradeRepository tradeRepository;
    private final StateMachineService<XoAcceptStatus, XoAcceptEvent> xoMachineSvc;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendToXoIfExists(String tradeId, StateContext<TradeStatus, TradeEvent> state, XoAcceptEvent status) {
        log.info("Attempt to interact with higher level machine for trade {} / {} / {}", tradeId, state, status);
        tradeRepository.findById(tradeId)
                .ifPresent(trade -> {
                    if (null != trade.getXoOrder()) {
                        int orderId = trade.getXoOrder().getId();
                        String machineId = XO_OPPORTUNITY_PREFIX + orderId;
                        Message<XoAcceptEvent> msg = builder(orderId, state, status).build();
                        log.info("Sending request to {} / {}", machineId, msg);
                        xoMachineSvc.acquireStateMachine(machineId).sendEvent(msg);
                    }
                });
    }

    private MessageBuilder<XoAcceptEvent> builder(int orderId,
                                                  StateContext<TradeStatus, TradeEvent> state, XoAcceptEvent status) {
        return MessageBuilder
                .withPayload(status)
                .setHeader(XoAcceptEvent.ORDER_ID, orderId)
                .setHeader(XoAcceptEvent.MSG_ID, state.getMessageHeaders().get(TradeEvent.MSG_ID));
    }
}
