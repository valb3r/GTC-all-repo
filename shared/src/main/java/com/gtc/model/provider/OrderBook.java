package com.gtc.model.provider;

import lombok.Builder;
import lombok.Data;

/**
 * Created by Valentyn Berezin on 01.01.18.
 */
@Data
@Builder
public class OrderBook {

    private ByClientAndCurrency meta;

    private double bestBuy;

    private double bestSell;

    private double amountBestBuy;

    private double amountBestSell;

    private int bidCount;

    private AggregatedOrder[] histogramSell;

    private AggregatedOrder[] histogramBuy;
}
