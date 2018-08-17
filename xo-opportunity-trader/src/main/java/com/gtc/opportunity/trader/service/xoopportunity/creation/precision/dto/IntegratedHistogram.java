package com.gtc.opportunity.trader.service.xoopportunity.creation.precision.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 07.04.18.
 */
@Data
public class IntegratedHistogram {

    private final long stepPrice;
    private final long minPrice;
    private final long maxPrice;
    private final boolean isInverted;

    private final long scale;

    // in position-asc order
    private final long[] amount;

    public long amount(long price) {
        return isInverted ? inverted(price) : straight(price);
    }

    private long straight(long price) {
        if (price >= maxPrice) {
            return amount[amount.length - 1];
        }

        if (price < minPrice) {
            return 0;
        }

        int pos = (int) ((price - minPrice) / stepPrice);
        long min = 0;
        long max = amount[pos];
        if (pos - 1 >= 0) {
            min = amount[pos - 1];
        }

        return min + (long) ((double) (price - minPrice - pos * stepPrice) / stepPrice * (max - min));
    }

    private long inverted(long price) {
        if (price > maxPrice) {
            return 0;
        }

        if (price <= minPrice) {
            return amount[amount.length - 1];
        }

        int pos = (int) ((maxPrice - price) / stepPrice);
        long min = 0;
        long max = amount[pos];
        if (pos - 1 >= 0) {
            min = amount[pos - 1];
        }

        return min + (long) ((double) (maxPrice - price - pos * stepPrice) / stepPrice * (max - min));
    }
}
