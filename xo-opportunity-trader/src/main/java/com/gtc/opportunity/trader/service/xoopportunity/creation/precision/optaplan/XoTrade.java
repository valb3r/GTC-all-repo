package com.gtc.opportunity.trader.service.xoopportunity.creation.precision.optaplan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import static com.gtc.opportunity.trader.service.xoopportunity.creation.precision.optaplan.XoTradeBalance.*;

/**
 * Using direct method so that Optaplanner will use lambda accessor.
 */
@Data
@PlanningEntity
@NoArgsConstructor
@AllArgsConstructor
public class XoTrade {

    private Long sellAmountFrom;
    private Long buyAmountTo;
    private Long sellPriceFrom;
    private Long buyPriceTo;

    @PlanningVariable(valueRangeProviderRefs = SELL_FROM_RANGE)
    public Long getSellAmountFrom() {
        return sellAmountFrom;
    }

    @PlanningVariable(valueRangeProviderRefs = BUY_TO_RANGE)
    public Long getBuyAmountTo() {
        return buyAmountTo;
    }

    @PlanningVariable(valueRangeProviderRefs = SELL_FROM_PRICE_RANGE)
    public Long getSellPriceFrom() {
        return sellPriceFrom;
    }

    @PlanningVariable(valueRangeProviderRefs = BUY_TO_PRICE_RANGE)
    public Long getBuyPriceTo() {
        return buyPriceTo;
    }
}
