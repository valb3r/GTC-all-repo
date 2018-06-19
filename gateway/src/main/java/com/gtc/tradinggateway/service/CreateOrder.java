package com.gtc.tradinggateway.service;

import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.dto.OrderCreatedDto;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Typically WSS based.
 */
public interface CreateOrder extends ClientNamed {

    /**
     * WS-based clients should return empty. REST - actual response.
     * @return created order descriptor.
     */
    Optional<OrderCreatedDto> create(String tryToAssignId,
                                    TradingCurrency from,
                                    TradingCurrency to,
                                    BigDecimal amount,
                                    BigDecimal price
    );
}
