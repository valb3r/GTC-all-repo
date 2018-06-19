package com.gtc.opportunity.trader.service.opportunity.creation.precision.dto;

import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;

/**
 * In market terms.
 */
@Data
@Builder
@AllArgsConstructor
public class XoTradeCondition {

    @NonNull
    private final Double minToSellAmount;

    @NonNull
    private final Double maxToSellAmount;

    @NonNull
    private final Double minFromBuyAmount;

    @NonNull
    private final Double maxFromBuyAmount;

    @NonNull
    private final Double minToSellPrice; // with safety limit incorporated

    @NonNull
    private final Double maxFromBuyPrice; // with safety limit incorporated

    @NonNull
    private final BigDecimal amountSafetyFromCoef; // i.e. 1.1 if margin is 10%

    @NonNull
    private final BigDecimal amountSafetyToCoef; // i.e. 1.1 if margin is 10%

    @NonNull
    private final BigDecimal lossFromCoef; // i.e. 0.998 if loss is 0.2%

    @NonNull
    private final BigDecimal lossToCoef; // i.e. 0.999 if loss is 0.1%

    @NonNull
    private final BigDecimal stepFromPricePow10;

    @NonNull
    private final BigDecimal stepToPricePow10;

    @NonNull
    private final BigDecimal stepFromAmountPow10;

    @NonNull
    private final BigDecimal stepToAmountPow10;

    @NonNull
    private final BigDecimal requiredProfitCoef; // i.e. 1.001 if we require at least 0.1%

    @NonNull
    private final Integer maxSolveTimeMs;

    @NonNull
    private final FullCrossMarketOpportunity.Histogram[] sellTo;

    @NonNull
    private final FullCrossMarketOpportunity.Histogram[] buyFrom;
}
