package com.gtc.opportunity.trader.service.opportunity.creation.precision.optaplan;

import com.gtc.opportunity.trader.service.opportunity.creation.precision.HistogramIntegrator;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.XoClientTradeConditionAsLong;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.optaplanner.core.api.domain.solution.PlanningEntityProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactProperty;
import org.optaplanner.core.api.domain.valuerange.CountableValueRange;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

/**
 * Using direct getter so that Optaplanner will use `fast` lambda accessor.
 */
@Data
@PlanningSolution
@NoArgsConstructor
@AllArgsConstructor
public class XoTradeBalance {

    public static final String SELL_FROM_RANGE = "sellFrom";
    public static final String BUY_TO_RANGE = "buyTo";
    public static final String SELL_FROM_PRICE_RANGE = "sellFromPrice";
    public static final String BUY_TO_PRICE_RANGE = "buyToPrice";

    private XoClientTradeConditionAsLong constraint;
    private XoTrade trade;

    private HardSoftLongScore score;

    private CountableValueRange<Long> sellFrom;
    private CountableValueRange<Long> buyTo;
    private CountableValueRange<Long> sellFromPrice;
    private CountableValueRange<Long> buyToPrice;

    private HistogramIntegrator integrator;

    @ProblemFactProperty
    public XoClientTradeConditionAsLong getConstraint() {
        return constraint;
    }

    @PlanningEntityProperty
    public XoTrade getTrade() {
        return trade;
    }

    @PlanningScore
    public HardSoftLongScore getScore() {
        return score;
    }

    @ValueRangeProvider(id = SELL_FROM_RANGE)
    public CountableValueRange<Long> getSellFrom() {
        return sellFrom;
    }

    @ValueRangeProvider(id = BUY_TO_RANGE)
    public CountableValueRange<Long> getBuyTo() {
        return buyTo;
    }

    @ValueRangeProvider(id = SELL_FROM_PRICE_RANGE)
    public CountableValueRange<Long> getSellFromPrice() {
        return sellFromPrice;
    }

    @ValueRangeProvider(id = BUY_TO_PRICE_RANGE)
    public CountableValueRange<Long> getBuyToPrice() {
        return buyToPrice;
    }

    public HistogramIntegrator getIntegrator() {
        return integrator;
    }
}
