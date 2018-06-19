package com.gtc.opportunity.trader.service.opportunity.replenishment.precision.optaplan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.optaplanner.core.api.domain.solution.PlanningEntityProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactProperty;
import org.optaplanner.core.api.domain.valuerange.CountableValueRange;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 26.03.18.
 */
@Data
@PlanningSolution
@NoArgsConstructor
@AllArgsConstructor
public class XoTradeBalance {

    public static final String BUY_FROM_RANGE = "buyFrom";
    public static final String SELL_TO_RANGE = "sellTo";
    public static final String BUY_FROM_PRICE_RANGE = "buyFromPrice";
    public static final String SELL_TO_PRICE_RANGE = "sellToPrice";

    @ProblemFactProperty
    private XoReplenishPrice price;

    @PlanningEntityProperty
    private XoTrade trade;

    @PlanningScore
    private HardSoftBigDecimalScore score;

    @ValueRangeProvider(id = BUY_FROM_RANGE)
    private CountableValueRange<BigDecimal> buyFrom;

    @ValueRangeProvider(id = SELL_TO_RANGE)
    private CountableValueRange<BigDecimal> sellTo;

    @ValueRangeProvider(id = BUY_FROM_PRICE_RANGE)
    private CountableValueRange<BigDecimal> buyFromPrice;

    @ValueRangeProvider(id = SELL_TO_PRICE_RANGE)
    private CountableValueRange<BigDecimal> sellToPrice;
}
