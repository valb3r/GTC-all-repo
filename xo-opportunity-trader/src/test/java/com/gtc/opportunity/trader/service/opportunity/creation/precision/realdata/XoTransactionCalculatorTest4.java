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
public class XoTransactionCalculatorTest4 extends BaseMockitoTest {

    private XoTransactionCalculator calculator = new XoTransactionCalculator(
            new ToLongMathMapper(),
            new HistogramIntegrator()
    );

    private XoTradeCondition condition;

    @Before
    public void init() {
        condition = new XoTradeCondition(
                "K1", 1000.0,
                0.019609579161763046, 1.0, 0.01, 1.0, 0.050995485, 0.05125436,
                new BigDecimal("1.1"), new BigDecimal("1.1"),
                new BigDecimal("0.998"), new BigDecimal("0.999"),
                new BigDecimal("0.00001"), new BigDecimal("0.000001"), new BigDecimal("0.00001"),
                new BigDecimal("0.001"), new BigDecimal("1.001"), 75, // 75ms is enough for 99.8% cases
                new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(0.051414600000000005, 0.051464, -5.0600000000000005),
                        new FullCrossMarketOpportunity.Histogram(0.0513652, 0.051414600000000005, -1.689),
                        new FullCrossMarketOpportunity.Histogram(0.0513158, 0.0513652, -0.04),
                        new FullCrossMarketOpportunity.Histogram(0.051266400000000004, 0.0513158, -0.529),
                        new FullCrossMarketOpportunity.Histogram(0.051217, 0.051266400000000004, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0511676, 0.051217, -61.048),
                        new FullCrossMarketOpportunity.Histogram(0.0511182, 0.0511676, -8.354),
                        new FullCrossMarketOpportunity.Histogram(0.051068800000000004, 0.0511182, -1.7249999999999999),
                        new FullCrossMarketOpportunity.Histogram(0.0510194, 0.051068800000000004, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.05097, 0.0510194, -2.266)
                },
                new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(0.05077, 0.050821000000000005, 0.31487771),
                        new FullCrossMarketOpportunity.Histogram(0.050821000000000005, 0.050872, 1.3644796700000001),
                        new FullCrossMarketOpportunity.Histogram(0.050872, 0.050923, 0.01122),
                        new FullCrossMarketOpportunity.Histogram(0.050923, 0.050974, 0.12047617),
                        new FullCrossMarketOpportunity.Histogram(0.050974, 0.051025, 6.39635034),
                        new FullCrossMarketOpportunity.Histogram(0.051025, 0.051076, 0.011),
                        new FullCrossMarketOpportunity.Histogram(0.051076, 0.051127, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.051127, 0.051178, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.051178, 0.051229, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.051229, 0.05128, 0.30818591)
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
