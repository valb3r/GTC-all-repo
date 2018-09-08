package com.gtc.opportunity.trader.service.statemachine.nnaccept;

import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.model.gateway.data.OrderStatus;
import com.gtc.model.gateway.response.manage.GetOrderResponse;
import com.gtc.opportunity.trader.BaseNnTradeInitialized;
import com.gtc.opportunity.trader.domain.*;
import com.gtc.opportunity.trader.repository.SoftCancelConfigRepository;
import com.gtc.opportunity.trader.repository.SoftCancelRepository;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayResponseListener;
import com.gtc.opportunity.trader.service.scheduled.trade.management.NnSlaveOrderPusher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static com.gtc.opportunity.trader.config.Const.Common.NN_OPPORTUNITY_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Created by Valentyn Berezin on 03.08.18.
 */
@Transactional(propagation = Propagation.NEVER)
public class NnAcceptMachineIT extends BaseNnTradeInitialized {

    @Autowired
    private WsGatewayResponseListener responseListener;

    @Autowired
    private StateMachineService<NnAcceptStatus, AcceptEvent> nnMachineSvc;

    @Autowired
    private StateMachineService<TradeStatus, TradeEvent> tradeMachineSvc;

    @Autowired
    private NnSlaveOrderPusher pusher;

    @Autowired
    private SoftCancelConfigRepository cancelConfigRepository;

    @Autowired
    private SoftCancelRepository cancelRepository;

    @Autowired
    private TransactionTemplate template;

    @MockBean
    private WsGatewayCommander commander;

    @Captor
    private ArgumentCaptor<CreateOrderCommand> captor;

    @BeforeEach
    public void initialize() {
        tradeMachineSvc.acquireStateMachine(TRADE_ONE).sendEvent(TradeEvent.DEPENDENCY_DONE);
        // FIXME: Without this wrapping and finding client entity again we would get detached entity exception;
        template.execute(status -> {
            SoftCancelConfig cancelConfig = SoftCancelConfig.builder()
                    .clientCfg(configRepository.findById(createdConfig.getId()).get())
                    .waitM(30)
                    .enabled(true)
                    .build();
            cancelConfigRepository.save(cancelConfig);
            return null;
        });
    }

    @AfterEach
    public void cleanup() {
        tradeMachineSvc.releaseStateMachine(TRADE_ONE, true);
        tradeMachineSvc.releaseStateMachine(TRADE_TWO, true);
        nnMachineSvc.releaseStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), true);
    }

    @Test
    public void ackOne() {
        doAck(TRADE_ONE, OrderStatus.NEW);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnStateMachine();
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.MASTER_OPENED);
        assertThat(acceptedRepository.findById(createdNnTrade.getId()))
                .map(AcceptedNnTrade::getStatus).contains(NnAcceptStatus.MASTER_OPENED);
        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.OPENED);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.DEPENDS_ON);
    }

    @Test
    public void ackCancelled() {
        doAck(TRADE_ONE, OrderStatus.NEW);
        doAck(TRADE_ONE, OrderStatus.CANCELED);
        doAck(TRADE_TWO, OrderStatus.CANCELED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnStateMachine();

        verify(commander, never()).createOrder(any(CreateOrderCommand.class));
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.ABORTED);
        assertThat(acceptedRepository.findById(createdNnTrade.getId()))
                .map(AcceptedNnTrade::getStatus).contains(NnAcceptStatus.ABORTED);
        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.CANCELLED);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.CANCELLED);
        assertSoftCancel(0, 0);
    }

    @Test
    public void ackDoneOne() {
        doAck(TRADE_ONE, OrderStatus.NEW);
        doAck(TRADE_ONE, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnStateMachine();

        verify(commander, never()).createOrder(any(CreateOrderCommand.class));
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.PENDING_SLAVE);
        assertThat(acceptedRepository.findById(createdNnTrade.getId()))
                .map(AcceptedNnTrade::getStatus).contains(NnAcceptStatus.PENDING_SLAVE);
        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.CLOSED);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.DEPENDS_ON);
    }

    @Test
    public void doneOne() {
        doAck(TRADE_ONE, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnStateMachine();

        verify(commander, never()).createOrder(any(CreateOrderCommand.class));
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.PENDING_SLAVE);
        assertThat(acceptedRepository.findById(createdNnTrade.getId()))
                .map(AcceptedNnTrade::getStatus).contains(NnAcceptStatus.PENDING_SLAVE);
        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.CLOSED);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.DEPENDS_ON);
    }

    @Test
    public void ackDoneOneAckTwo() {
        doAck(TRADE_ONE, OrderStatus.NEW);
        doAck(TRADE_ONE, OrderStatus.FILLED);
        pusher.pushOrders();
        doAck(TRADE_TWO, OrderStatus.NEW);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnStateMachine();
        verify(commander).createOrder(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(TRADE_TWO);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.SLAVE_OPENED);
        assertThat(acceptedRepository.findById(createdNnTrade.getId()))
                .map(AcceptedNnTrade::getStatus).contains(NnAcceptStatus.SLAVE_OPENED);
        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.CLOSED);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.OPENED);
    }

    @Test
    public void ackDoneOneAckDoneTwo() {
        doAck(TRADE_ONE, OrderStatus.NEW);
        doAck(TRADE_ONE, OrderStatus.FILLED);
        pusher.pushOrders();
        doAck(TRADE_TWO, OrderStatus.NEW);
        doAck(TRADE_TWO, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnStateMachine();
        verify(commander).createOrder(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(TRADE_TWO);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.DONE);
        assertThat(acceptedRepository.findById(createdNnTrade.getId()))
                .map(AcceptedNnTrade::getStatus).contains(NnAcceptStatus.DONE);
        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.CLOSED);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.CLOSED);
        assertSoftCancel(1, 0);
    }

    @Test
    public void doneOneDoneTwo() {
        doAck(TRADE_ONE, OrderStatus.NEW);
        doAck(TRADE_ONE, OrderStatus.FILLED);
        pusher.pushOrders();
        doAck(TRADE_TWO, OrderStatus.FILLED);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnStateMachine();
        verify(commander).createOrder(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(TRADE_TWO);
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.DONE);
        assertThat(acceptedRepository.findById(createdNnTrade.getId()))
                .map(AcceptedNnTrade::getStatus).contains(NnAcceptStatus.DONE);
        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.CLOSED);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.CLOSED);
        assertSoftCancel(1, 0);
    }

    @Test
    public void ackDoneOneAckTwoWhenExpired() {
        doAck(TRADE_ONE, OrderStatus.NEW);
        doAck(TRADE_ONE, OrderStatus.FILLED);
        Trade trade = tradeRepository.findById(TRADE_TWO).get();
        trade.setRecordedOn(trade.getRecordedOn().minusMinutes(createdNnConfig.getMaxSlaveDelayM() + 1L));
        tradeRepository.save(trade);
        pusher.pushOrders();
        doAck(TRADE_TWO, OrderStatus.NEW);

        StateMachine<NnAcceptStatus, AcceptEvent> machine = nnStateMachine();
        verify(commander, never()).createOrder(captor.capture());
        assertThat(machine.getState().getId()).isEqualTo(NnAcceptStatus.ABORTED);
        assertThat(acceptedRepository.findById(createdNnTrade.getId()))
                .map(AcceptedNnTrade::getStatus).contains(NnAcceptStatus.ABORTED);
        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.CLOSED);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.CANCELLED);
        assertSoftCancel(0, 1);
    }

    private void assertSoftCancel(int doneSlaves, int cancelledSlaves) {
        if (doneSlaves == 0 && cancelledSlaves == 0) {
            assertThat(cancelRepository.findForTrade(tradeRepository.findById(TRADE_TWO).get())).isNotPresent();
            return;
        }

        assertThat(cancelRepository.findForTrade(tradeRepository.findById(TRADE_TWO).get()))
                .map(SoftCancel::getDone).contains(doneSlaves);
        assertThat(cancelRepository.findForTrade(tradeRepository.findById(TRADE_TWO).get()))
                .map(SoftCancel::getCancelled).contains(cancelledSlaves);
    }

    private StateMachine<NnAcceptStatus, AcceptEvent> nnStateMachine() {
        return nnMachineSvc.acquireStateMachine(NN_OPPORTUNITY_PREFIX + createdNnTrade.getId(), false);
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
