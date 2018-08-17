package com.gtc.opportunity.trader.service.trade;

import com.gtc.model.gateway.data.OrderStatus;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayResponseListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.service.StateMachineService;

/**
 * Creates trade and its dependent. Then tries to create 3,4th trades that should fail due to low balance
 * as it is locked by master and slave,
 * then ack/done master and 4th trade passes and 5th fails (master amount released).
 * then cancels master and 4th trade passes and 5th passes (both amounts are released).
 */
public class TradeDependentLockingIT extends BaseLockingIT {

    @Autowired
    private StateMachineService<TradeStatus, TradeEvent> stateMachineService;

    @Autowired
    private WsGatewayResponseListener listener;

    @Test
    public void testAck() {
        Trade base = tryCreateAndShouldCreate();
        tryCreateAndShouldCreate(base, true);
        tryCreateAndShouldNotCreate();
        tryCreateAndShouldNotCreate(true);

        listener.byId(response(base.getId(), OrderStatus.NEW));

        tryCreateAndShouldCreate();
        tryCreateAndShouldNotCreate(true);
    }

    @Test
    public void testDone() {
        Trade base = tryCreateAndShouldCreate();
        tryCreateAndShouldCreate(base, true);
        tryCreateAndShouldNotCreate();
        tryCreateAndShouldNotCreate(true);

        listener.byId(response(base.getId(), OrderStatus.FILLED));

        tryCreateAndShouldCreate();
        tryCreateAndShouldNotCreate(true);
    }

    @Test
    public void testCancelled() {
        Trade base = tryCreateAndShouldCreate();
        tryCreateAndShouldCreate(base, true);
        tryCreateAndShouldNotCreate();
        tryCreateAndShouldNotCreate(true);

        listener.byId(response(base.getId(), OrderStatus.CANCELED));

        tryCreateAndShouldCreate();
        tryCreateAndShouldCreate(null, true);
    }

    @Test
    public void testTimeout() {
        Trade base = tryCreateAndShouldCreate();
        tryCreateAndShouldCreate(base, true);
        tryCreateAndShouldNotCreate();
        tryCreateAndShouldNotCreate(true);

        stateMachineService.acquireStateMachine(base.getId()).sendEvent(TradeEvent.TIMEOUT);

        tryCreateAndShouldCreate();
        tryCreateAndShouldCreate(null, true);
    }

    @Test
    public void testError() {
        Trade base = tryCreateAndShouldCreate();
        tryCreateAndShouldCreate(base, true);
        tryCreateAndShouldNotCreate();
        tryCreateAndShouldNotCreate(true);

        stateMachineService.acquireStateMachine(base.getId()).sendEvent(TradeEvent.ERROR);

        tryCreateAndShouldCreate();
        tryCreateAndShouldCreate(null, true);
    }
}
