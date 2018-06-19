package com.gtc.meta;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 29.12.17.
 */
@Data
public class CurrencyPair {

    private TradingCurrency from;
    private TradingCurrency to;

    public CurrencyPair(TradingCurrency from, TradingCurrency to) {
        this.from = from;
        this.to = to;
    }

    public CurrencyPair invert() {
        return new CurrencyPair(to, from);
    }
}
