package com.gtc.opportunity.trader.service.opportunity.creation.precision.optaplan;

import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.HistogramIntegrator;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.AsFixed;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.IntegratedHistogram;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.XoClientTradeConditionAsLong;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 *  | soft:
 *  | -sellFromAmount + buyToAmount * lossTo >= profit * sellFromAmount (profit = profitPct/100) = profit eq = max.
 * <
 *  | hard:
 *  | sellFromAmount * sellFromPrice * lossFrom - buyToAmount * buyToPrice >= 0 = no loss of paired currency
 *  |
 *  | amountHistogram(minSellFromPrice, sellFromPrice) >= sellFromAmount * (1 + amountSafetyFromCoef)
 *  | amountHistogram(minBuyToPrice, buyToPrice) >= buyToAmount * (1 + amountSafetyToCoef)
 */
@Slf4j
@RequiredArgsConstructor
public class XoBalanceScore implements EasyScoreCalculator<XoTradeBalance> {

    private static final long MIN_VAL = -100000;

    private final Map<XoClientTradeConditionAsLong, Coeffs> coeffs = new ConcurrentHashMap<>();

    @Override
    public HardSoftLongScore calculateScore(XoTradeBalance bal) {
        Coeffs coef = coeffs.computeIfAbsent(bal.getConstraint(),
                trade -> new Coeffs(
                        trade,
                        bal.getConstraint().getMarketBuyFrom(),
                        bal.getConstraint().getMarketSellTo(),
                        bal.getIntegrator())
        );

        long profit = calculateProfit(bal, coef);
        long noLoss = calculateNoLoss(bal, coef);
        long profitPositive = calculateHasProfit(profit);
        long hasSellAmount = histogramHasAmount(
                bal.getTrade().getSellPriceFrom(), bal.getTrade().getSellAmountFrom() * coef.getAmountSafetyFromCoef(),
                coef.getSellFrom());
        long hasBuyAmount = histogramHasAmount(
                bal.getTrade().getBuyPriceTo(), bal.getTrade().getBuyAmountTo() * coef.getAmountSafetyToCoef(),
                coef.getBuyTo());
        long hasMaxSellPrice = hasCorrectMaxSellPrice(
                bal.getTrade().getSellPriceFrom(),
                bal.getConstraint().getMaxFromSellPrice().getVal()
        );
        long hasMinBuyPrice = hasCorrectMinBuyPrice(
                bal.getTrade().getBuyPriceTo(),
                bal.getConstraint().getMinToBuyPrice().getVal()
        );

        return HardSoftLongScore.valueOf(
                noLoss + profitPositive + hasSellAmount + hasBuyAmount + hasMaxSellPrice + hasMinBuyPrice,
                profit
        );
    }

    private long calculateNoLoss(XoTradeBalance bal, Coeffs coef) {
        long loss = bal.getTrade().getSellAmountFrom() * bal.getTrade().getSellPriceFrom() * coef.getLossFromEq2()
                - bal.getTrade().getBuyAmountTo() * bal.getTrade().getBuyPriceTo() * coef.getScaleCoefEq2();

        return loss > 0 ? 0 : loss;
    }

    private long calculateHasProfit(long profit) {
        return profit > 0 ? 0 : profit;
    }

    private long calculateProfit(XoTradeBalance bal, Coeffs coef) {
        return bal.getTrade().getBuyAmountTo() * coef.getLossToEq1()
                - bal.getTrade().getSellAmountFrom() * coef.getProfitCoefEq1();
    }

    private long histogramHasAmount(long price, long amount, IntegratedHistogram histogram) {
        long avail = histogram.amount(price);
        long missing = avail - amount;
        return missing > 0 ? 0 : missing;
    }

    private long hasCorrectMaxSellPrice(long sellPrice, long maxPrice) {
        if (sellPrice <= maxPrice) {
            return 0;
        }

        long delta = maxPrice - sellPrice;
        return delta > MIN_VAL ? delta : MIN_VAL;
    }

    private long hasCorrectMinBuyPrice(long buyPrice, long minPrice) {
        if (buyPrice >= minPrice) {
            return 0;
        }

        long delta = buyPrice - minPrice;
        return delta > MIN_VAL ? delta : MIN_VAL;
    }

    @Getter
    private static class Coeffs {

        private final long lossToEq1;
        private final long profitCoefEq1; // scaled (1 + profit)

        private final long lossFromEq2;
        private final long scaleCoefEq2;

        private final long amountSafetyFromCoef;
        private final long amountSafetyToCoef;
        private final IntegratedHistogram sellFrom;
        private final IntegratedHistogram buyTo;

        Coeffs(XoClientTradeConditionAsLong bal,
               FullCrossMarketOpportunity.Histogram[] marketBuyFrom,
               FullCrossMarketOpportunity.Histogram[] marketSellTo,
               HistogramIntegrator integrator) {
            // buyToAmount * lossTo - sellFromAmount * profitCoef >= 0
            // profitCoef = 1 + db value / 100
            int eq1scale = getEq1Scale(bal);
            lossToEq1 = scaled(bal.getLossToCoef(), eq1scale - getEq1Coef1Scale(bal));
            profitCoefEq1 = scaled(bal.getMinProfitCoef(), eq1scale - getEq1Coef2Scale(bal));
            // sellFromAmount * sellFromPrice * lossFrom - buyToAmount * buyToPrice >= 0 = no loss of paired currency
            int eq2scale = getEq2Scale(bal);
            lossFromEq2 = scaled(bal.getLossFromCoef(), eq2scale - getEq2Coef1Scale(bal));
            scaleCoefEq2 = BigDecimal.ONE.movePointRight(eq2scale - getEq2Coef2Scale(bal)).longValueExact();

            // amountHistogram(minSellFromPrice, sellFromPrice) >= sellFromAmount * amountSafetyFromCoef
            // amountSafetyFromCoef = 1 + db value / 100
            int eq3scale = bal.getMinFromSellAmount().getScale() + bal.getAmountSafetyFromCoef().getScale();
            amountSafetyFromCoef = scaled(bal.getAmountSafetyFromCoef(), 0);
            sellFrom = integrator.integrate(marketBuyFrom, getScale(bal.getMaxFromSellPrice()), eq3scale, false);

            // amountHistogram(minBuyToPrice, buyToPrice) >= buyToAmount * amountSafetyToCoef
            // amountSafetyToCoef = 1 + db value / 100
            int eq4scale = bal.getMinToBuyAmount().getScale() + bal.getAmountSafetyToCoef().getScale();
            amountSafetyToCoef = scaled(bal.getAmountSafetyToCoef(), 0);
            buyTo = integrator.integrate(marketSellTo, getScale(bal.getMinToBuyPrice()), eq4scale, true);
        }

        private static short getEq1Scale(XoClientTradeConditionAsLong bal) {
            return (short) IntStream.of(
                    getEq1Coef1Scale(bal),
                    getEq1Coef2Scale(bal)
            ).max().orElse(0);
        }

        private static int getEq1Coef1Scale(XoClientTradeConditionAsLong bal) {
            return bal.getMinToBuyAmount().getScale() + bal.getLossToCoef().getScale();
        }

        private static int getEq1Coef2Scale(XoClientTradeConditionAsLong bal) {
            return bal.getMinFromSellAmount().getScale() + bal.getMinProfitCoef().getScale();
        }

        private static int getEq2Scale(XoClientTradeConditionAsLong bal) {
            return IntStream.of(
                    getEq2Coef1Scale(bal),
                    getEq2Coef2Scale(bal)
            ).max().orElse(0);
        }

        private static int getEq2Coef1Scale(XoClientTradeConditionAsLong bal) {
            return bal.getMinFromSellAmount().getScale() + bal.getMaxFromSellPrice().getScale() +
                    bal.getLossFromCoef().getScale();
        }

        private static int getEq2Coef2Scale(XoClientTradeConditionAsLong bal) {
            return bal.getMinToBuyAmount().getScale() + bal.getMinToBuyPrice().getScale();
        }

        private static int getScale(AsFixed... values) {
            return Arrays.stream(values).mapToInt(AsFixed::getScale).max().orElse(0);
        }

        private static long scaled(AsFixed orig, int pow10) {
            if (0 == pow10) {
                return orig.getVal();
            }

            return orig.getVal() * BigDecimal.ONE.movePointRight(pow10).longValueExact();
        }
    }
}
