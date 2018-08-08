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
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class XoTransactionCalculatorTest extends BaseMockitoTest {

    private XoTransactionCalculator calculator = new XoTransactionCalculator(
            new ToLongMathMapper(),
            new HistogramIntegrator()
    );

    private XoTradeCondition condition;

    @Before
    public void init() {
        condition = new XoTradeCondition(
                "K1", 1000.0,
                0.01, 0.1, 0.02228772013595108, 0.1, 0.044682234, 0.0448677565,
                new BigDecimal("1.01"), new BigDecimal("1.01"),
                new BigDecimal("0.999"), new BigDecimal("0.998"),
                new BigDecimal("0.000001"), new BigDecimal("0.00001"), new BigDecimal("0.001"),
                new BigDecimal("0.00001"), new BigDecimal("1.0008"), 75, // 75ms is enough for 99.8% cases
                new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(0.045076, 0.04512, -2.32105433),
                        new FullCrossMarketOpportunity.Histogram(0.045032, 0.045076, -0.28522804),
                        new FullCrossMarketOpportunity.Histogram(0.044988, 0.045032, -0.66084247),
                        new FullCrossMarketOpportunity.Histogram(0.044944, 0.044988, -0.42454),
                        new FullCrossMarketOpportunity.Histogram(0.044899999999999995, 0.044944, -1.22803055),
                        new FullCrossMarketOpportunity.Histogram(0.044856, 0.044899999999999995, -0.77354284),
                        new FullCrossMarketOpportunity.Histogram(0.044812, 0.044856, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.044767999999999995, 0.044812, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.044724, 0.044767999999999995, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.04468, 0.044724, -0.98509921)
                },
                new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(0.044498, 0.044535200000000004, 46.17),
                        new FullCrossMarketOpportunity.Histogram(0.044535200000000004, 0.044572400000000005, 9.98),
                        new FullCrossMarketOpportunity.Histogram(0.044572400000000005, 0.0446096, 6.829),
                        new FullCrossMarketOpportunity.Histogram(0.0446096, 0.0446468, 5.612),
                        new FullCrossMarketOpportunity.Histogram(0.0446468, 0.044684, 4.478),
                        new FullCrossMarketOpportunity.Histogram(0.044684, 0.0447212, 2.427),
                        new FullCrossMarketOpportunity.Histogram(0.0447212, 0.044758400000000004, 0.379),
                        new FullCrossMarketOpportunity.Histogram(0.044758400000000004, 0.0447956, 10.195),
                        new FullCrossMarketOpportunity.Histogram(0.0447956, 0.0448328, 1.638),
                        new FullCrossMarketOpportunity.Histogram(0.0448328, 0.04487, 8.75)
                }
        );
    }

    @Test
    public void calculate() {
        WarmupUtil.warmup(() -> calculator.calculate(condition), 20, RejectionException.class);

        PreciseXoAmountDto amountDto = calculator.calculate(condition);

        assertThat(amountDto.getProfitPct()).isGreaterThanOrEqualTo(0.08);
        assertThat(amountDto.getSellPrice()).isLessThanOrEqualTo(new BigDecimal("0.044868"));
        assertThat(amountDto.getBuyPrice()).isGreaterThanOrEqualTo(new BigDecimal("0.04462"));
    }
}
