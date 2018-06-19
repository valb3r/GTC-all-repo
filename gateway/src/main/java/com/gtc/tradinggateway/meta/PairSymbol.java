package com.gtc.tradinggateway.meta;

import lombok.Data;

import java.util.Optional;

/**
 * Created by mikro on 02.02.2018.
 */
@Data
public class PairSymbol {

    private TradingCurrency from;

    private TradingCurrency to;

    private String symbol;

    private Boolean isInverted = false;

    public PairSymbol invert() {
        return new PairSymbol(to, from, symbol, !isInverted);
    }

    public PairSymbol(TradingCurrency from, TradingCurrency to, String symbol) {
        this.from = from;
        this.to = to;
        this.symbol = symbol;
    }

    public PairSymbol(TradingCurrency from, TradingCurrency to, String symbol, Boolean isInverted) {
        this(from, to, symbol);
        this.isInverted = isInverted;
    }

    public String toString() {
        return symbol;
    }
}
