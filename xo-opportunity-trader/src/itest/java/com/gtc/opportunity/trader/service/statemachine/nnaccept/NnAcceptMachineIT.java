package com.gtc.opportunity.trader.service.statemachine.nnaccept;

import com.gtc.model.gateway.data.OrderDto;
import com.gtc.model.gateway.data.OrderStatus;
import com.gtc.model.gateway.response.manage.GetOrderResponse;
import com.gtc.opportunity.trader.BaseNnTradeInitialized;
import com.gtc.opportunity.trader.domain.AcceptEvent;
import com.gtc.opportunity.trader.domain.NnAcceptStatus;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayResponseListener;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;

import java.math.BigDecimal;

import static com.gtc.opportunity.trader.config.Const.Common.NN_OPPORTUNITY_PREFIX;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Created by Valentyn Berezin on 03.08.18.
 */
public class NnAcceptMachineIT extends BaseNnTradeInitialized {

    @Autowired
    WsGatewayResponseListener responseListener;

    @Autowired
    private StateMachineService<NnAcceptStatus, AcceptEvent> nnMachineSvc;

    @Autowired
    private StateMachineService<TradeStatus, TradeEvent> tradeMachineSvc;

    @After
    public void cleanup() {
        tradeMachineSvc.releaseStateMachine(TRADE_ONE, true);
        tradeMachineSvc.releaseStateMachine(TRADE_TWO, true);
        nnMachineSvc.releaseStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), true);
    }

    @Test
    public void ackOne() {
        doAck(TRADE_ONE, OrderStatus.NEW);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.ACK_PART);
    }

    @Test
    public void ackTwo() {
        doAck(TRADE_ONE, OrderStatus.NEW);
        doAck(TRADE_TWO, OrderStatus.NEW);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.ACK_BOTH);
    }

    @Test
    public void doneOne() {
        doAck(TRADE_ONE, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.DONE_PART);
    }

    @Test
    public void doneOneOneOpen() {
        doAck(TRADE_ONE, OrderStatus.FILLED);
        doAck(TRADE_TWO, OrderStatus.NEW);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.DONE_PART);
    }

    @Test
    public void oneOpenOneDone() {
        doAck(TRADE_TWO, OrderStatus.NEW);
        doAck(TRADE_ONE, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.DONE_PART);
    }

    @Test
    public void doneTwoAndCheckDoneField() {
        doAck(TRADE_TWO, OrderStatus.FILLED);
        doAck(TRADE_ONE, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.DONE);
    }

    @Test
    public void openAckDoneTwo() {
        doAck(TRADE_TWO, OrderStatus.NEW);
        doAck(TRADE_TWO, OrderStatus.FILLED);
        doAck(TRADE_ONE, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.DONE);
    }

    @Test
    public void openAckPartFillFillTwo() {
        doAck(TRADE_TWO, OrderStatus.NEW);
        doAck(TRADE_TWO, OrderStatus.PARTIALLY_FILLED);
        doAck(TRADE_TWO, OrderStatus.FILLED);
        doAck(TRADE_ONE, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.DONE);
    }

    @Test
    public void openPartFillFillTwo() {
        doAck(TRADE_TWO, OrderStatus.PARTIALLY_FILLED);
        doAck(TRADE_ONE, OrderStatus.PARTIALLY_FILLED);
        doAck(TRADE_TWO, OrderStatus.FILLED);
        doAck(TRADE_ONE, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.DONE);
    }

    private void doAck(String orderId, OrderStatus status) {
        responseListener.byId(GetOrderResponse.builder()
                .clientName(CLIENT)
                .id(orderId)
                .order(OrderDto.builder()
                        .orderId(orderId)
                        .status(status)
                        .size(BigDecimal.ONE)
                        .price(BigDecimal.ONE)
                        .statusString("")
                        .build())
                .build());
    }
}
