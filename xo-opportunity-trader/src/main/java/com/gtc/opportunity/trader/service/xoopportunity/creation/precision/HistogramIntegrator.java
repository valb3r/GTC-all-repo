package com.gtc.opportunity.trader.service.xoopportunity.creation.precision;

import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.service.xoopportunity.creation.precision.dto.IntegratedHistogram;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * Created by Valentyn Berezin on 07.04.18.
 */
@Service
public class HistogramIntegrator {

    // typically we use scale provided by exchange here, also amount sign will be discarded
    // integrated histogram is ordered from min to max
    // also order of histogram will be reversed - from best buy/sell prices
    public IntegratedHistogram integrate(FullCrossMarketOpportunity.Histogram[] histogram, int priceScale,
                                         int amountScale, boolean isMarketSell) {
        if (0 == histogram.length) {
            throw new IllegalStateException("Empty histogram");
        }

        double min = minPrice(histogram);
        double max = maxPrice(histogram);
        double origStep = (max - min) / histogram.length;

        double[] inOrder = new double[histogram.length];
        for (FullCrossMarketOpportunity.Histogram entry : histogram) {
            int pos = (int) (((entry.getMinPrice() + entry.getMaxPrice()) / 2.0 - min) / origStep);
            inOrder[pos] = Math.abs(entry.getAmount());
        }

        long[] asLong = new long[histogram.length];
        long step = RoundingUtil.longVal(BigDecimal.valueOf(origStep), priceScale, RoundingMode.CEILING);
        BigDecimal summ = BigDecimal.ZERO;
        int start = isMarketSell ? 0 : histogram.length - 1;
        int end = isMarketSell ? histogram.length : 0;
        int iStep = isMarketSell ? 1 : -1;

        for (int i = start; isMarketSell ?  i < end : i >= 0; i += iStep) {
            summ = summ.add(BigDecimal.valueOf(inOrder[i]));
            asLong[isMarketSell ? i : start - i] = RoundingUtil.longVal(summ, amountScale, RoundingMode.FLOOR);
        }

        long minLong = RoundingUtil.longVal(BigDecimal.valueOf(min), priceScale, RoundingMode.FLOOR);
        long maxLong = minLong + step * histogram.length;
        return new IntegratedHistogram(step, minLong, maxLong, !isMarketSell, priceScale, asLong);
    }

    private static double minPrice(FullCrossMarketOpportunity.Histogram[] histogram) {
        return Arrays.stream(histogram)
                .mapToDouble(FullCrossMarketOpportunity.Histogram::getMinPrice)
                .min().orElse(0.0);
    }

    private static double maxPrice(FullCrossMarketOpportunity.Histogram[] histogram) {
        return Arrays.stream(histogram)
                .mapToDouble(FullCrossMarketOpportunity.Histogram::getMaxPrice)
                .max().orElse(0.0);
    }
}
