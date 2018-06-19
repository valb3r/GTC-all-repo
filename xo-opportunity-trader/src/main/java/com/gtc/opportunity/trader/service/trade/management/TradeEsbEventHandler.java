package com.gtc.opportunity.trader.service.trade.management;

import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 28.02.18.
 */
@Service
@RequiredArgsConstructor
public class TradeEsbEventHandler {

    private final StateMachineService<TradeStatus, TradeEvent> stateMachineService;
    private final TradeRepository tradeRepository;

    @Transactional
    public void ackError(Trade.EsbKey key, String source, String error) {
        tradeRepository.findByAssignedId(key).ifPresent(order ->
            stateMachineService.acquireStateMachine(order.getId())
                    .sendEvent(MessageBuilder
                            .withPayload(TradeEvent.ERROR)
                            .setHeader(TradeEvent.DATA, error)
                            .setHeader(TradeEvent.MSG_ID, source)
                            .build())
        );
    }

    @Transactional
    public void ackTransientError(Trade.EsbKey key, String source, String error) {
        tradeRepository.findByAssignedId(key).ifPresent(order ->
                stateMachineService.acquireStateMachine(order.getId())
                .sendEvent(MessageBuilder
                        .withPayload(TradeEvent.TRANSIENT_ERR)
                        .setHeader(TradeEvent.DATA, error)
                        .setHeader(TradeEvent.MSG_ID, source)
                        .build())
        );
    }

    @Transactional
    public void ackOrder(Trade.EsbKey key, String source, String status, String nativeStatus,
                         BigDecimal amount, BigDecimal price) {
        tradeRepository.findByAssignedId(key).ifPresent(order -> {
            StateMachine<TradeStatus, TradeEvent> machine = stateMachineService.acquireStateMachine(order.getId());
            machine.sendEvent(MessageBuilder
                    .withPayload(TradeEvent.ACK)
                    .setHeader(TradeEvent.AMOUNT, amount)
                    .setHeader(TradeEvent.PRICE, price)
                    .setHeader(TradeEvent.STATUS, status)
                    .setHeader(TradeEvent.NATIVE_STATUS, nativeStatus)
                    .setHeader(TradeEvent.MSG_ID, source)
                    .build());
        });
    }

    @Transactional
    public void ackDone(Trade.EsbKey key, String source, String status, String nativeStatus) {
        tradeRepository.findByAssignedId(key).ifPresent(order -> {
            StateMachine<TradeStatus, TradeEvent> machine = stateMachineService.acquireStateMachine(order.getId());
            machine.sendEvent(MessageBuilder
                    .withPayload(TradeEvent.DONE)
                    .setHeader(TradeEvent.STATUS, status)
                    .setHeader(TradeEvent.NATIVE_STATUS, nativeStatus)
                    .setHeader(TradeEvent.MSG_ID, source)
                    .setHeader(TradeEvent.AMOUNT, BigDecimal.ZERO)
                    .build());
        });
    }

    @Transactional
    public void ackCancel(Trade.EsbKey key, String source, String status, String nativeStatus) {
        tradeRepository.findByAssignedId(key).ifPresent(order -> {
            StateMachine<TradeStatus, TradeEvent> machine = stateMachineService.acquireStateMachine(order.getId());
            machine.sendEvent(MessageBuilder
                    .withPayload(TradeEvent.CANCELLED)
                    .setHeader(TradeEvent.STATUS, status)
                    .setHeader(TradeEvent.NATIVE_STATUS, nativeStatus)
                    .setHeader(TradeEvent.MSG_ID, source)
                    .build());
        });
    }

    @Transactional
    public void ackCreate(String requestedId, String source, String status, String nativeStatus, Trade.EsbKey key) {
        tradeRepository.findById(requestedId).ifPresent(order -> {
            order.setAssignedId(key.getAssignedId());
            StateMachine<TradeStatus, TradeEvent> machine = stateMachineService.acquireStateMachine(order.getId());
            machine.sendEvent(MessageBuilder
                    .withPayload(TradeEvent.ACK)
                    .setHeader(TradeEvent.STATUS, status)
                    .setHeader(TradeEvent.NATIVE_STATUS, nativeStatus)
                    .setHeader(TradeEvent.MSG_ID, source)
                    .build());
        });
    }

    @Transactional
    public void ackCreateAndDone(String reqId, String source, String status, String nativeStatus, Trade.EsbKey key) {
        tradeRepository.findById(reqId).ifPresent(order -> {
            order.setAssignedId(key.getAssignedId() + "." + order.getId());
            StateMachine<TradeStatus, TradeEvent> machine = stateMachineService.acquireStateMachine(order.getId());
            machine.sendEvent(MessageBuilder
                    .withPayload(TradeEvent.ACK)
                    .setHeader(TradeEvent.STATUS, status)
                    .setHeader(TradeEvent.NATIVE_STATUS, nativeStatus)
                    .setHeader(TradeEvent.MSG_ID, source)
                    .build());

            machine.sendEvent(MessageBuilder
                    .withPayload(TradeEvent.DONE)
                    .setHeader(TradeEvent.STATUS, status)
                    .setHeader(TradeEvent.NATIVE_STATUS, nativeStatus)
                    .setHeader(TradeEvent.MSG_ID, source)
                    .build());
        });
    }
}
