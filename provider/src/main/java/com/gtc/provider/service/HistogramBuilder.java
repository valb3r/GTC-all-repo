package com.gtc.provider.service;

import com.gtc.provider.config.WriteConf;
import com.gtc.model.provider.AggregatedOrder;
import com.gtc.model.provider.Bid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistogramBuilder {

    private final WriteConf writeConf;

    AggregatedOrder[] buildHistogram(List<Bid> market, boolean isSell) {
        Stat stat = computeMinMax(market);
        double min = stat.getMin();
        double max = stat.getMax();
        int resolution = writeConf.getHistogram().getResolution();
        double step = (max - min) / resolution;

        AggregatedOrder[] entries = new AggregatedOrder[resolution];

        for (int pos = 0; pos < resolution; ++pos) {
            initializeHistogramEntry(isSell, min, resolution, step, entries, pos);
        }

        double[] avgTime = new double[resolution];

        for (Bid bid : market) {
            int pos;
            // only 1 bid available on a side
            if (0.0 == step && bid.getAmount() > 0) {
                pos = resolution - 1;
            } else {
                pos = (int) (((bid.getPriceMin() + bid.getPriceMax()) / 2.0 - min) / step);
            }

            if (pos >= resolution) {
                pos = resolution - 1;
            }
            AggregatedOrder entry = entries[pos];
            entry.setAmount(entry.getAmount() + bid.getAmount());
            entry.setBidCount(entry.getBidCount() + 1);
            long time = bid.getTimestamp();
            entry.setOldestBid(oldest(time, entry.getOldestBid()));
            entry.setLatestBid(latest(time, entry.getLatestBid()));
            avgTime[pos] += bid.getTimestamp();
        }

        for (int pos = 0; pos < resolution; ++pos) {
            AggregatedOrder entry = entries[pos];
            double avg = avgTime[pos] / entry.getBidCount();
            entry.setAverageBid((long) avg);
        }

        return entries;
    }

    private void initializeHistogramEntry(boolean isSell, double min, int resolution, double step,
                                          AggregatedOrder[] entries, int pos) {
        AggregatedOrder entry = new AggregatedOrder();
        entry.setMinPrice(min + pos * step);
        entry.setMaxPrice(min + (pos + 1) * step);
        entry.setSell(isSell);
        // -1 and 1 enclose virtual 0 (ticker)
        entry.setPosId(isSell ? (short) (pos + 1) : (short) -(resolution - pos));
        entries[pos] = entry;
    }

    private long oldest(long one, long two) {
        return Math.min(one, two);
    }

    private long latest(long one, long two) {
        return Math.max(one, two);
    }

    private Stat computeMinMax(List<Bid> market) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (Bid bid : market) {
            if (bid.getPriceMin() < min) {
                min = bid.getPriceMin();
            }

            if (bid.getPriceMax() > max) {
                max = bid.getPriceMax();
            }
        }

        return new Stat(min, max);
    }

    @Data
    private static class Stat {

        private final double min;
        private final double max;
    }
}
