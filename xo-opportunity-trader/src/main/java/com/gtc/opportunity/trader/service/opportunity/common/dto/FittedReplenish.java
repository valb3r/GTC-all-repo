package com.gtc.opportunity.trader.service.opportunity.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Sell - to, buy - from.
 */
@Getter
@Builder
@ToString
@AllArgsConstructor
public class FittedReplenish {

    private final BigDecimal targetSellPrice;
    private final BigDecimal targetBuyPrice;

    private final BigDecimal minSellAmount;
    private final BigDecimal maxSellAmount;

    private final BigDecimal minBuyAmount;
    private final BigDecimal maxBuyAmount;

    private final BigDecimal amountGridStepSell;
    private final BigDecimal amountGridStepBuy;

    private final BigDecimal priceGridStepSell;
    private final BigDecimal priceGridStepBuy;
}
