package com.gtc.opportunity.trader.service.trade;

import com.gtc.model.gateway.data.OrderDto;
import com.gtc.model.gateway.data.OrderStatus;
import com.gtc.model.gateway.response.manage.GetOrderResponse;
import com.gtc.opportunity.trader.BaseInitializedIT;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.service.TradeCreationService;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.RejectionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.MathContext;

import static com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.Reason.LOW_BAL;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by Valentyn Berezin on 11.08.18.
 */
public abstract class BaseLockingIT extends BaseInitializedIT {

    @Autowired
    protected TradeCreationService creationService;

    protected GetOrderResponse response(String id, OrderStatus status) {
        OrderDto dto = OrderDto.builder()
                .orderId(id).price(BigDecimal.TEN).size(BigDecimal.ONE).status(status)
                .build();

        return new GetOrderResponse(CLIENT, id, dto);
    }

    protected Trade tryCreateAndShouldCreate() {
        return create();
    }

    protected Trade tryCreateAndShouldCreate(Trade dependsOn, boolean isSell) {
        return create(dependsOn, isSell);
    }

    protected void tryCreateAndShouldNotCreate() {
        tryCreateAndShouldNotCreate(false);
    }

    protected void tryCreateAndShouldNotCreate(boolean isSell) {
        tryCreateAndShouldNotCreate(null, isSell);
    }

    protected void tryCreateAndShouldNotCreate(Trade dependsOn, boolean isSell) {
        assertThrows(RejectionException.class, () -> create(dependsOn, isSell), LOW_BAL.getMsg());
    }

    protected Trade create() {
        return create(null, false);
    }

    protected Trade create(Trade dependsOn, boolean isSell) {
        return creationService.createTradeNoSideValidation(
                dependsOn,
                createdConfig,
                BigDecimal.ONE,
                null == dependsOn ?
                        BigDecimal.TEN
                        .divide(
                                BigDecimal.ONE.subtract(TRADE_CHARGE_RATE_PCT.movePointLeft(2)),
                                MathContext.DECIMAL128) :
                        BigDecimal.TEN,
                isSell
        ).getTrade();
    }
}
