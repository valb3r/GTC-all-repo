package com.gtc.opportunity.trader.service.xoopportunity.creation.precision.realdata;

import com.gtc.opportunity.trader.BaseMockitoTest;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.service.dto.PreciseXoAmountDto;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.RejectionException;
import com.gtc.opportunity.trader.service.xoopportunity.creation.precision.HistogramIntegrator;
import com.gtc.opportunity.trader.service.xoopportunity.creation.precision.ToLongMathMapper;
import com.gtc.opportunity.trader.service.xoopportunity.creation.precision.WarmupUtil;
import com.gtc.opportunity.trader.service.xoopportunity.creation.precision.XoTransactionCalculator;
import com.gtc.opportunity.trader.service.xoopportunity.creation.precision.dto.XoTradeCondition;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class XoTransactionCalculatorTest6 extends BaseMockitoTest {

    private XoTransactionCalculator calculator = new XoTransactionCalculator(
            new ToLongMathMapper(),
            new HistogramIntegrator()
    );

    private XoTradeCondition condition;

    @BeforeEach
    public void init() {
        condition = new XoTradeCondition(
                "K1", 1000.0,
                0.658579347404768, 3.543, 1.0, 3.543, 0.00151841992, 0.001524,
                new BigDecimal("1.1"), new BigDecimal("1.1"),
                new BigDecimal("0.999"), new BigDecimal("0.999"),
                new BigDecimal("0.000001"), new BigDecimal("0.0000001"), new BigDecimal("0.001"),
                new BigDecimal("0.01"), new BigDecimal("1.001"), 75, // 75ms is enough for 99.8% cases
                new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(0.0015166, 0.0015181099999999998, -176.83999999999997),
                        new FullCrossMarketOpportunity.Histogram(0.0015181099999999998, 0.00151962, -345.2700000000001),
                        new FullCrossMarketOpportunity.Histogram(0.00151962, 0.0015211299999999999, -58.290000000000006),
                        new FullCrossMarketOpportunity.Histogram(0.0015211299999999999, 0.00152264, -1284.1100000000001),
                        new FullCrossMarketOpportunity.Histogram(0.00152264, 0.00152415, -1933.9099999999999),
                        new FullCrossMarketOpportunity.Histogram(0.00152415, 0.0015256599999999999, -424.44000000000005),
                        new FullCrossMarketOpportunity.Histogram(0.0015256599999999999, 0.00152717, -568.5500000000001),
                        new FullCrossMarketOpportunity.Histogram(0.00152717, 0.00152868, -548.31),
                        new FullCrossMarketOpportunity.Histogram(0.00152868, 0.00153019, -3950.7000000000003),
                        new FullCrossMarketOpportunity.Histogram(0.00153019, 0.0015317, -5958.64)
                },
                new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(0.00151, 0.0015115, 775.554),
                        new FullCrossMarketOpportunity.Histogram(0.0015115, 0.001513, 5675.577),
                        new FullCrossMarketOpportunity.Histogram(0.001513, 0.0015145, 589.89),
                        new FullCrossMarketOpportunity.Histogram(0.0015145, 0.001516, 5668.34),
                        new FullCrossMarketOpportunity.Histogram(0.001516, 0.0015175000000000002, 274.31),
                        new FullCrossMarketOpportunity.Histogram(0.0015175000000000002, 0.0015190000000000002, 2718.296),
                        new FullCrossMarketOpportunity.Histogram(0.0015190000000000002, 0.0015205000000000002, 189.366),
                        new FullCrossMarketOpportunity.Histogram(0.0015205000000000002, 0.0015220000000000001, 1257.2440000000001),
                        new FullCrossMarketOpportunity.Histogram(0.0015220000000000001, 0.0015235000000000001, 50.543),
                        new FullCrossMarketOpportunity.Histogram(0.0015235000000000001, 0.001525, 5046.451)
                }
        );
    }

    @Test
    public void calculate() {
        WarmupUtil.warmup(() -> calculator.calculate(condition), 10, RejectionException.class);

        PreciseXoAmountDto amountDto = calculator.calculate(condition);

        assertThat(amountDto.getProfitPct()).isGreaterThanOrEqualTo(0.15);
        assertThat(amountDto.getSellPrice()).isLessThanOrEqualTo(new BigDecimal("0.001524"));
        assertThat(amountDto.getBuyPrice()).isGreaterThanOrEqualTo(new BigDecimal("0.0015184"));
    }
}
