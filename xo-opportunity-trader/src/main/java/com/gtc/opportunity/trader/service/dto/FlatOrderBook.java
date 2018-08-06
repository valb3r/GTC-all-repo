package com.gtc.opportunity.trader.service.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 28.07.18.
 */
@Data
public class FlatOrderBook {

    private final long timestamp;
    private final float bestSell;
    private final float bestBuy;
    private final float histogramBuyStep;
    private final float histogramSellStep;
    private final float[] histogramBuy;
    private final float[] histogramSell;
}
