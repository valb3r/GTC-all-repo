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
public class XoTransactionCalculatorTest5 extends BaseMockitoTest {

    private XoTransactionCalculator calculator = new XoTransactionCalculator(
            new ToLongMathMapper(),
            new HistogramIntegrator()
    );

    private XoTradeCondition condition;

    @Before
    public void init() {
        condition = new XoTradeCondition(
                "K1", 1000.0,
                100.0, 1000.0, 10.0, 1000.0, 2.3961974999999997E-5, 2.408795E-5,
                new BigDecimal("1.1"), new BigDecimal("1.1"),
                new BigDecimal("0.998"), new BigDecimal("0.999"),
                new BigDecimal("0.00000001"), new BigDecimal("0.00000001"), new BigDecimal("0.01"),
                new BigDecimal("100"), new BigDecimal("1.001"), 75, // 75ms is enough for 99.8% cases
                new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(2.395E-5, 2.3970999999999998E-5, -600.0),
                        new FullCrossMarketOpportunity.Histogram(2.3970999999999998E-5, 2.3992E-5, 0.0),
                        new FullCrossMarketOpportunity.Histogram(2.3992E-5, 2.4013E-5, -1700.0),
                        new FullCrossMarketOpportunity.Histogram(2.4013E-5, 2.4034E-5, 0.0),
                        new FullCrossMarketOpportunity.Histogram(2.4034E-5, 2.4055E-5, -500.0),
                        new FullCrossMarketOpportunity.Histogram(2.4055E-5, 2.4075999999999998E-5, 0.0),
                        new FullCrossMarketOpportunity.Histogram(2.4075999999999998E-5, 2.4097E-5, -600.0),
                        new FullCrossMarketOpportunity.Histogram(2.4097E-5, 2.4118E-5, -100.0),
                        new FullCrossMarketOpportunity.Histogram(2.4118E-5, 2.4139E-5, -1000.0),
                        new FullCrossMarketOpportunity.Histogram(2.4139E-5, 2.416E-5, -2000.0)
                },
                new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(2.388E-5, 2.3902E-5, 35449.47),
                        new FullCrossMarketOpportunity.Histogram(2.3902E-5, 2.3924E-5, 0.0),
                        new FullCrossMarketOpportunity.Histogram(2.3924E-5, 2.3945999999999997E-5, 0.0),
                        new FullCrossMarketOpportunity.Histogram(2.3945999999999997E-5, 2.3967999999999998E-5, 10425.35),
                        new FullCrossMarketOpportunity.Histogram(2.3967999999999998E-5, 2.399E-5, 41.73),
                        new FullCrossMarketOpportunity.Histogram(2.399E-5, 2.4012E-5, 77213.87),
                        new FullCrossMarketOpportunity.Histogram(2.4012E-5, 2.4034E-5, 0.0),
                        new FullCrossMarketOpportunity.Histogram(2.4034E-5, 2.4055999999999998E-5, 2358.45),
                        new FullCrossMarketOpportunity.Histogram(2.4055999999999998E-5, 2.4078E-5, 0.0),
                        new FullCrossMarketOpportunity.Histogram(2.4078E-5, 2.41E-5, 1089.1)
                }
        );
    }

    @Test
    public void calculate() {
        WarmupUtil.warmup(() -> calculator.calculate(condition), 10, RejectionException.class);

        PreciseXoAmountDto amountDto = calculator.calculate(condition);

        assertThat(amountDto.getProfitPct()).isGreaterThanOrEqualTo(0.135);
    }
}
