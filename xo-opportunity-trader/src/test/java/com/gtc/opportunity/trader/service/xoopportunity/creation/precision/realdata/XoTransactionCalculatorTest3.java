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
public class XoTransactionCalculatorTest3 extends BaseMockitoTest {

    private XoTransactionCalculator calculator = new XoTransactionCalculator(
            new ToLongMathMapper(),
            new HistogramIntegrator()
    );

    private XoTradeCondition condition;

    @BeforeEach
    public void init() {
        condition = new XoTradeCondition(
                "K1", 1000.0,
                0.30214638750757633, 3.0, 1.0E-4, 3.0, 0.003309654, 0.0033373305,
                new BigDecimal("1.1"), new BigDecimal("1.1"),
                new BigDecimal("0.998"), new BigDecimal("0.999"),
                new BigDecimal("0.000001"), new BigDecimal("0.000001"), new BigDecimal("0.0001"),
                new BigDecimal("0.01"), new BigDecimal("1.001"), 75, // 75ms is enough for 99.8% cases
                new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(0.0033377, 0.003341, -21.17),
                        new FullCrossMarketOpportunity.Histogram(0.0033344, 0.0033377, -0.68),
                        new FullCrossMarketOpportunity.Histogram(0.0033311, 0.0033344, -5.55),
                        new FullCrossMarketOpportunity.Histogram(0.0033278, 0.0033311, -34.300000000000004),
                        new FullCrossMarketOpportunity.Histogram(0.0033245, 0.0033278, -0.97),
                        new FullCrossMarketOpportunity.Histogram(0.0033212, 0.0033245, -0.13),
                        new FullCrossMarketOpportunity.Histogram(0.0033179, 0.0033212, -3.62),
                        new FullCrossMarketOpportunity.Histogram(0.0033146, 0.0033179, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0033113, 0.0033146, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.003308, 0.0033113, -2.27)
                },
                new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(0.00332, 0.0033219, 44.84),
                        new FullCrossMarketOpportunity.Histogram(0.0033219, 0.0033238, 32.649891093843394),
                        new FullCrossMarketOpportunity.Histogram(0.0033238, 0.0033257, 19.8),
                        new FullCrossMarketOpportunity.Histogram(0.0033257, 0.0033276, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0033276, 0.0033295, 73.92),
                        new FullCrossMarketOpportunity.Histogram(0.0033295, 0.0033314, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0033314, 0.0033333, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0033333, 0.0033352, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0033352, 0.0033371, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.0033371, 0.003339, 0.4126)
                }
        );
    }

    @Test
    public void calculate() {
        WarmupUtil.warmup(() -> calculator.calculate(condition), 10, RejectionException.class);

        PreciseXoAmountDto amountDto = calculator.calculate(condition);

        assertThat(amountDto.getProfitPct()).isGreaterThanOrEqualTo(0.2);
    }
}
