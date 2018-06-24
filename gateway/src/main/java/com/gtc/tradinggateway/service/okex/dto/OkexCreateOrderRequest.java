package com.gtc.tradinggateway.service.okex.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 24.06.18.
 */
@Data
public class OkexCreateOrderRequest {

    private final String symbol;
    private final String type;
    private final BigDecimal price;
    private final BigDecimal amount;
}
