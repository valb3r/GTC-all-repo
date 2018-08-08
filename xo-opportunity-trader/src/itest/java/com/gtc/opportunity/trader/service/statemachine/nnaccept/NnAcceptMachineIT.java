package com.gtc.opportunity.trader.service.statemachine.nnaccept;

import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.model.gateway.data.OrderStatus;
import com.gtc.model.gateway.response.manage.GetOrderResponse;
import com.gtc.opportunity.trader.BaseNnTradeInitialized;
import com.gtc.opportunity.trader.domain.AcceptEvent;
import com.gtc.opportunity.trader.domain.NnAcceptStatus;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayResponseListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;

import java.math.BigDecimal;

import static com.gtc.opportunity.trader.config.Const.Common.NN_OPPORTUNITY_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Created by Valentyn Berezin on 03.08.18.
 */
public class NnAcceptMachineIT extends BaseNnTradeInitialized {

    @Autowired
    private WsGatewayResponseListener responseListener;

    @Autowired
    private StateMachineService<NnAcceptStatus, AcceptEvent> nnMachineSvc;

    @Autowired
    private StateMachineService<TradeStatus, TradeEvent> tradeMachineSvc;

    @MockBean
    private WsGatewayCommander commander;

    @Captor
    private ArgumentCaptor<CreateOrderCommand> captor;

    @Before
    public void initialize() {
        tradeMachineSvc.acquireStateMachine(TRADE_ONE).sendEvent(TradeEvent.DEPENDENCY_DONE);
    }

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
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.MASTER_OPENED);
    }

    @Test
    public void ackDoneOne() {
        doAck(TRADE_ONE, OrderStatus.NEW);
        doAck(TRADE_ONE, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        verify(commander).createOrder(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(TRADE_TWO);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.SLAVE_UNKNOWN);
        assertThat(tradeRepository.findById(TRADE_TWO).get().getStatus()).isEqualTo(TradeStatus.UNKNOWN);
    }

    @Test
    public void doneOne() {
        doAck(TRADE_ONE, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        verify(commander).createOrder(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(TRADE_TWO);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.SLAVE_UNKNOWN);
        assertThat(tradeRepository.findById(TRADE_TWO).get().getStatus()).isEqualTo(TradeStatus.UNKNOWN);
    }

    @Test
    public void ackDoneOneAckTwo() {
        doAck(TRADE_ONE, OrderStatus.NEW);
        doAck(TRADE_ONE, OrderStatus.FILLED);
        doAck(TRADE_TWO, OrderStatus.NEW);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        verify(commander).createOrder(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(TRADE_TWO);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.SLAVE_OPENED);
    }

    @Test
    public void ackDoneOneAckDoneTwo() {
        doAck(TRADE_ONE, OrderStatus.NEW);
        doAck(TRADE_ONE, OrderStatus.FILLED);
        doAck(TRADE_TWO, OrderStatus.NEW);
        doAck(TRADE_TWO, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        verify(commander).createOrder(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(TRADE_TWO);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.DONE);
    }

    @Test
    public void doneOneDoneTwo() {
        doAck(TRADE_ONE, OrderStatus.NEW);
        doAck(TRADE_ONE, OrderStatus.FILLED);
        doAck(TRADE_TWO, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnMachineSvc
                .acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
        verify(commander).createOrder(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(TRADE_TWO);
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
