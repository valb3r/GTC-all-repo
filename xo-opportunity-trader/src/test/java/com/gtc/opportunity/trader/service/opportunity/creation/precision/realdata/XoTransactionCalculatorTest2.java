package com.gtc.opportunity.trader.service.opportunity.creation.precision.realdata;

import com.gtc.opportunity.trader.BaseMockitoTest;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.service.dto.PreciseXoAmountDto;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.RejectionException;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.HistogramIntegrator;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.ToLongMathMapper;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.WarmupUtil;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.XoTransactionCalculator;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.XoTradeCondition;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class XoTransactionCalculatorTest2 extends BaseMockitoTest {

    private XoTransactionCalculator calculator = new XoTransactionCalculator(
            new ToLongMathMapper(),
            new HistogramIntegrator()
    );

    private XoTradeCondition condition;

    @Before
    public void init() {
        condition = new XoTradeCondition(
                "K1", 1000.0,
                1.0e-4, 5.0, 0.30690191721627685, 5.0, 0.0032409, 0.0032569,
                new BigDecimal("1.1"), new BigDecimal("1.1"),
                new BigDecimal("0.999"), new BigDecimal("0.998"),
                new BigDecimal("0.000001"), new BigDecimal("0.000001"), new BigDecimal("0.01"),
                new BigDecimal("0.0001"), new BigDecimal("1.001"), 75, // 75ms is enough for 99.8% cases
                new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(0.0032561, 0.003258, -10.0),
                        new FullCrossMarketOpportunity.Histogram(0.0032542, 0.0032561, -52.8),
                        new FullCrossMarketOpportunity.Histogram(0.0032523, 0.0032542, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0032504, 0.0032523, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0032485, 0.0032504, -15.0),
                        new FullCrossMarketOpportunity.Histogram(0.0032466, 0.0032485, -1.8985),
                        new FullCrossMarketOpportunity.Histogram(0.0032447, 0.0032466, -19.8),
                        new FullCrossMarketOpportunity.Histogram(0.0032428, 0.0032447, -26.4),
                        new FullCrossMarketOpportunity.Histogram(0.0032409, 0.0032428, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.003239, 0.0032409, -16.2031)
                },
                new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(0.003229, 0.0032321, 141.25),
                        new FullCrossMarketOpportunity.Histogram(0.0032321, 0.0032352, 400.59000000000003),
                        new FullCrossMarketOpportunity.Histogram(0.0032352, 0.0032383, 0.97),
                        new FullCrossMarketOpportunity.Histogram(0.0032383, 0.0032414, 14.9),
                        new FullCrossMarketOpportunity.Histogram(0.0032414, 0.0032445, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0032445, 0.0032476, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0032476, 0.0032507, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0032507, 0.0032538, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0032538, 0.0032569, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0032569, 0.00326, 105.47)
                }
        );
    }

    @Test
    public void calculate() {
        WarmupUtil.warmup(() -> calculator.calculate(condition), 10, RejectionException.class);

        PreciseXoAmountDto amountDto = calculator.calculate(condition);

        assertThat(amountDto.getProfitPct()).isGreaterThanOrEqualTo(0.15);
    }
}
