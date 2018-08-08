package com.gtc.opportunity.trader.service.xoopportunity.creation.precision.dto;

import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * In client terms.
 */
@Data
@Builder(toBuilder = true)
public class XoClientTradeConditionAsLong {

    @NonNull
    private final AsFixed minToBuyAmount;

    @NonNull
    private final AsFixed maxToBuyAmount;

    @NonNull
    private final AsFixed minFromSellAmount;

    @NonNull
    private final AsFixed maxFromSellAmount;

    @NonNull
    private final AsFixed minToBuyPrice; // with safety limit incorporated

    @NonNull
    private final AsFixed maxFromSellPrice; // with safety limit incorporated

    @NonNull
    private final AsFixed amountSafetyFromCoef; // i.e. 1.1 if margin is 10%

    @NonNull
    private final AsFixed amountSafetyToCoef; // i.e. 1.1 if margin is 10%

    @NonNull
    private final AsFixed lossFromCoef;

    @NonNull
    private final AsFixed lossToCoef;

    @NonNull
    private final AsFixed minProfitCoef;

    @NonNull
    private final FullCrossMarketOpportunity.Histogram[] marketSellTo;

    @NonNull
    private final FullCrossMarketOpportunity.Histogram[] marketBuyFrom;
}
