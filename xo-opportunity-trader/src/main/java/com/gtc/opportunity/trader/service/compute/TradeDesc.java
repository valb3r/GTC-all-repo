package com.gtc.opportunity.trader.service.compute;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 04.09.18.
 */
@Data
public class TradeDesc {

    private final BigDecimal price;
    private final BigDecimal amount;
    private final boolean isSell;

    public TradeDesc(BigDecimal price, BigDecimal amount, boolean isSell) {
        this.price = price;
        this.amount = amount.abs();
        this.isSell = isSell;
    }
}
