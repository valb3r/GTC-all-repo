package com.gtc.opportunity.trader.service.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Sell from, buy to.
 */
@Data
public class PreciseXoAmountDto {

    private final BigDecimal sellPrice;
    private final BigDecimal sellAmount;
    private final BigDecimal buyPrice;
    private final BigDecimal buyAmount;
    private final BigDecimal profit;
    private final double profitPct;
}
