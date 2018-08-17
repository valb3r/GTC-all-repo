package com.gtc.opportunity.trader.service.xoopportunity.creation.precision;

import com.gtc.opportunity.trader.BaseMockitoTest;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.service.xoopportunity.creation.precision.dto.IntegratedHistogram;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Created by Valentyn Berezin on 11.04.18.
 */
public class HistogramIntegratorTest extends BaseMockitoTest  {

    private FullCrossMarketOpportunity.Histogram[] sell;
    private FullCrossMarketOpportunity.Histogram[] buy;

    private HistogramIntegrator integrator = new HistogramIntegrator();

    @BeforeEach
    public void init() {
        sell = new FullCrossMarketOpportunity.Histogram[] {
                new FullCrossMarketOpportunity.Histogram(25, 30, 8),
                new FullCrossMarketOpportunity.Histogram(10, 15, 5),
                new FullCrossMarketOpportunity.Histogram(20, 25, 10),
                new FullCrossMarketOpportunity.Histogram(15, 20, 0),
        };

        buy = new FullCrossMarketOpportunity.Histogram[] {
                new FullCrossMarketOpportunity.Histogram(8, 10, 3),
                new FullCrossMarketOpportunity.Histogram(4, 6, 0),
                new FullCrossMarketOpportunity.Histogram(6, 8, 5)
        };
    }

    @Test
    public void integrateSell0Scale() {
        IntegratedHistogram res = integrator.integrate(sell, 0, 0, true);

        assertThat(res.getAmount()).containsExactly(5, 5, 15, 23);
        assertThat(res.getMinPrice()).isEqualTo(10);
        assertThat(res.getMaxPrice()).isEqualTo(30);

        assertThat(res.amount(9)).isEqualTo(0);
        assertThat(res.amount(10)).isEqualTo(0);
        assertThat(res.amount(11)).isEqualTo(1);
        assertThat(res.amount(16)).isEqualTo(5);
        assertThat(res.amount(24)).isEqualTo(13);
        assertThat(res.amount(25)).isEqualTo(15);
        assertThat(res.amount(30)).isEqualTo(23);
        assertThat(res.amount(35)).isEqualTo(23);
    }

    @Test
    public void integrateBuy0Scale() {
        IntegratedHistogram res = integrator.integrate(buy, 0, 0, false);

        assertThat(res.getAmount()).containsExactly(3, 8, 8);
        assertThat(res.getMinPrice()).isEqualTo(4);
        assertThat(res.getMaxPrice()).isEqualTo(10);

        assertThat(res.amount(11)).isEqualTo(0);
        assertThat(res.amount(10)).isEqualTo(0);
        assertThat(res.amount(9)).isEqualTo(1); // round down
        assertThat(res.amount(8)).isEqualTo(3);
        assertThat(res.amount(7)).isEqualTo(5); // round down
        assertThat(res.amount(6)).isEqualTo(8);
        assertThat(res.amount(4)).isEqualTo(8);
        assertThat(res.amount(2)).isEqualTo(8);
    }
}
