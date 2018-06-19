package com.gtc.tradinggateway.util;

import com.gtc.tradinggateway.meta.PairSymbol;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by Valentyn Berezin on 05.03.18.
 */
@UtilityClass
public class DefaultInvertHandler {

    private static final String SELL = "sell";
    private static final String BUY = "buy";

    private static final String SELL_UPPER = "SELL";
    private static final String BUY_UPPER = "BUY";

    public BigDecimal amountFromOrig(PairSymbol symbol, BigDecimal amount, BigDecimal price) {
        BigDecimal amountRes = amount;

        if (symbol.getIsInverted()) {
            amountRes = amount.negate().multiply(price);
        }

        return amountRes;
    }

    public BigDecimal priceFromOrig(PairSymbol symbol, BigDecimal price) {
        BigDecimal priceRes = price;

        if (symbol.getIsInverted()) {
            priceRes = BigDecimal.ONE.divide(price, price.scale(), RoundingMode.HALF_EVEN);
        }

        return priceRes;
    }

    public String amountToBuyOrSell(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) < 0 ? SELL : BUY;
    }

    public String amountToBuyOrSellUpper(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) < 0 ? SELL_UPPER : BUY_UPPER;
    }

    public BigDecimal mapFromBuyOrSell(String buyOrSell, BigDecimal amount) {
        return SELL.equalsIgnoreCase(buyOrSell) ? amount.negate() : amount;
    }
}
