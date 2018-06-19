package com.gtc.model.provider;

import lombok.Builder;
import lombok.Data;

/**
 * Created by Valentyn Berezin on 01.01.18.
 */
@Data
@Builder
public class Ticker {

    private ByClientAndCurrency meta;

    private double price;

    private Double minPrice;

    private Double maxPrice;

    private long tickerDate;
}
