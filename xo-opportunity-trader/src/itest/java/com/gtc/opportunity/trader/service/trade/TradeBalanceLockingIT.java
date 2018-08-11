package com.gtc.opportunity.trader.service.trade;

import com.gtc.model.gateway.data.OrderStatus;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayResponseListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

/**
 * We are going to create 4 trades, 1st should pass, shortly after first we create next which fails
 * because balance is low, then we ack/cancel/done/ack+done/ack+cancel 1st trade and create
 * 3rd trade which passes, try to create 4th trade and it fails
 */
@Transactional(propagation = Propagation.NEVER)
public class TradeBalanceLockingIT extends BaseLocking {

    @Autowired
    private WsGatewayResponseListener listener;

    @Test
    public void testAck() {
        handleSequence(id -> listener.byId(response(id, OrderStatus.NEW)));
    }

    @Test
    public void testCancel() {
        handleSequence(id -> listener.byId(response(id, OrderStatus.CANCELED)));
    }

    @Test
    public void testDone() {
        handleSequence(id -> listener.byId(response(id, OrderStatus.FILLED)));
    }

    @Test
    public void testAckDone() {
        handleSequence(id -> {
            listener.byId(response(id, OrderStatus.NEW));
            listener.byId(response(id, OrderStatus.FILLED));
        });
    }

    @Test
    public void testAckCancel() {
        handleSequence(id -> {
            listener.byId(response(id, OrderStatus.NEW));
            listener.byId(response(id, OrderStatus.CANCELED));
        });
    }

    private void handleSequence(Consumer<String> doAck) {
        String idOne = tryCreateAndShouldCreate().getId();
        tryCreateAndShouldNotCreate();
        doAck.accept(idOne);
        tryCreateAndShouldCreate();
        tryCreateAndShouldNotCreate();
    }
}
