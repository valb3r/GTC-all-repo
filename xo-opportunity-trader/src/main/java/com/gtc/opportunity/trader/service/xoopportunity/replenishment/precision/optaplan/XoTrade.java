package com.gtc.opportunity.trader.service.xoopportunity.replenishment.precision.optaplan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.math.BigDecimal;

import static com.gtc.opportunity.trader.service.xoopportunity.replenishment.precision.optaplan.XoTradeBalance.*;

/**
 * Created by Valentyn Berezin on 26.03.18.
 */
@Data
@PlanningEntity
@NoArgsConstructor
@AllArgsConstructor
public class XoTrade {

    @PlanningVariable(valueRangeProviderRefs = BUY_FROM_RANGE)
    private BigDecimal buyAmountFrom = BigDecimal.ZERO;

    @PlanningVariable(valueRangeProviderRefs = SELL_TO_RANGE)
    private BigDecimal sellAmountTo = BigDecimal.ZERO;

    @PlanningVariable(valueRangeProviderRefs = BUY_FROM_PRICE_RANGE)
    private BigDecimal buyFromPrice = BigDecimal.ZERO;

    @PlanningVariable(valueRangeProviderRefs = SELL_TO_PRICE_RANGE)
    private BigDecimal sellToPrice = BigDecimal.ZERO;
}
