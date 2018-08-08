package com.gtc.opportunity.trader.service.xoopportunity.creation.precision.optaplan;

import com.gtc.opportunity.trader.BaseMockitoTest;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.service.xoopportunity.creation.precision.HistogramIntegrator;
import com.gtc.opportunity.trader.service.xoopportunity.creation.precision.dto.AsFixed;
import com.gtc.opportunity.trader.service.xoopportunity.creation.precision.dto.XoClientTradeConditionAsLong;
import org.junit.Before;
import org.junit.Test;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Valentyn Berezin on 11.04.18.
 */
public class XoBalanceScoreTest extends BaseMockitoTest {

    private XoBalanceScore balanceScore = new XoBalanceScore();

    private XoTradeBalance bal;

    private FullCrossMarketOpportunity.Histogram[] marketBuy = new FullCrossMarketOpportunity.Histogram[] {
            new FullCrossMarketOpportunity.Histogram(7000.0, 7100.0, 0.0),
            new FullCrossMarketOpportunity.Histogram(7100.0, 7200.0, 1.0)
    };

    private FullCrossMarketOpportunity.Histogram[] marketSell = new FullCrossMarketOpportunity.Histogram[] {
            new FullCrossMarketOpportunity.Histogram(6600, 6700, 1.0),
            new FullCrossMarketOpportunity.Histogram(6700, 6800, 0.0)
    };

    @Before
    public void init() {
        XoClientTradeConditionAsLong condition = XoClientTradeConditionAsLong.builder()
                .minToBuyAmount(new AsFixed(new BigDecimal("0.082894")))
                .maxToBuyAmount(new AsFixed(new BigDecimal("0.082894")))
                .minFromSellAmount(new AsFixed(new BigDecimal("0.08")))
                .maxFromSellAmount(new AsFixed(new BigDecimal("0.08")))
                .minToBuyPrice(new AsFixed(new BigDecimal("6797.1")))
                .maxFromSellPrice(new AsFixed(new BigDecimal("7051.24")))
                .amountSafetyFromCoef(new AsFixed(new BigDecimal("1.1")))
                .amountSafetyToCoef(new AsFixed(new BigDecimal("1.2")))
                .lossFromCoef(new AsFixed(new BigDecimal("0.999")))
                .lossToCoef(new AsFixed(new BigDecimal("0.998")))
                .minProfitCoef(new AsFixed(new BigDecimal("1.001")))
                .marketSellTo(marketSell)
                .marketBuyFrom(marketBuy)
                .build();
        XoTrade trade = new XoTrade(8L, 82894L, 705124L, 67971L);

        bal = new XoTradeBalance();
        bal.setConstraint(condition);
        bal.setTrade(trade);
        bal.setIntegrator(new HistogramIntegrator());
    }

    @Test
    public void calculateScoreSimpleHistogram() {
        HardSoftLongScore score = balanceScore.calculateScore(bal);

        assertThat(score.isFeasible()).isTrue();
        assertThat(score.getHardScore()).isEqualTo(0);
        assertThat(score.getSoftScore()).isEqualTo(2648212L);
    }

    @Test
    public void calculateScoreSimpleHistogramFailsLoss() {
        bal.getTrade().setBuyPriceTo(70000L);
        HardSoftLongScore score = balanceScore.calculateScore(bal);

        assertThat(score.isFeasible()).isFalse();
        assertThat(score.getHardScore()).isEqualTo(-167228992L);
        assertThat(score.getSoftScore()).isEqualTo(2648212L);
    }

    @Test
    public void calculateScoreSimpleHistogramFailsNoHistogramTo() {
        // requested amount x 1.2 - delta
        bal.setConstraint(bal.getConstraint().toBuilder()
                .marketSellTo(new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(6700, 6800, 0.0),
                        new FullCrossMarketOpportunity.Histogram(6600, 6700, 0.0994727),
                })
                .build()
        );

        HardSoftLongScore score = balanceScore.calculateScore(bal);

        assertThat(score.isFeasible()).isFalse();
        assertThat(score.getHardScore()).isEqualTo(-1L);
        assertThat(score.getSoftScore()).isEqualTo(2648212L);
    }

    @Test
    public void calculateScoreSimpleHistogramFailsNoHistogramFrom() {
        // requested amount x 1.1 - delta
        bal.setConstraint(bal.getConstraint().toBuilder()
                .marketBuyFrom(new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(7000.0, 7100.0, 0.0),
                        new FullCrossMarketOpportunity.Histogram(7100.0, 7200.0, 0.087)
                })
                .build()
        );

        HardSoftLongScore score = balanceScore.calculateScore(bal);

        assertThat(score.isFeasible()).isFalse();
        assertThat(score.getHardScore()).isEqualTo(-1L);
        assertThat(score.getSoftScore()).isEqualTo(2648212L);
    }

    @Test
    public void calculateScoreSimpleHistogramExactHistogramTo() {
        // requested amount x 1.2
        bal.setConstraint(bal.getConstraint().toBuilder()
                .marketSellTo(new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(6700, 6800, 0.0),
                        new FullCrossMarketOpportunity.Histogram(6600, 6700, 0.0994728),
                })
                .build()
        );

        HardSoftLongScore score = balanceScore.calculateScore(bal);

        assertThat(score.isFeasible()).isTrue();
        assertThat(score.getHardScore()).isEqualTo(0);
        assertThat(score.getSoftScore()).isEqualTo(2648212L);
    }

    @Test
    public void calculateScoreSimpleHistogramFailsExactHistogramFrom() {
        // requested amount x 1.1
        bal.setConstraint(bal.getConstraint().toBuilder()
                .marketBuyFrom(new FullCrossMarketOpportunity.Histogram[] {
                        new FullCrossMarketOpportunity.Histogram(7000.0, 7100.0, 0.0),
                        new FullCrossMarketOpportunity.Histogram(7100.0, 7200.0, 0.088)
                })
                .build()
        );

        HardSoftLongScore score = balanceScore.calculateScore(bal);

        assertThat(score.isFeasible()).isTrue();
        assertThat(score.getHardScore()).isEqualTo(0);
        assertThat(score.getSoftScore()).isEqualTo(2648212L);
    }
}
