package com.gtc.provider.clients;

import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;
import java.util.Map;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Data
public class MarketDto {

    private final Map<CurrencyPair, Collection<Bid>> market;
    private final Map<CurrencyPair, Ticker> ticker;

    @Data
    @AllArgsConstructor
    public static class Ticker {

        private long timestamp;
        private double value;
    }
}
