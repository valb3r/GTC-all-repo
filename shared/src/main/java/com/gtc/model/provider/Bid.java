package com.gtc.model.provider;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * If amount is negative -> it is ask.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Bid {

    private final String id;
    // > 0 Bid (Buy) else Ask (Sell)
    private final double amount;
    private final double priceMin;
    private final double priceMax;
    private final long timestamp = System.currentTimeMillis();

    public Bid(Bid other, double newAmount) {
        this.id = other.id;
        this.amount = newAmount;
        this.priceMin = other.priceMin;
        this.priceMax = other.priceMax;
    }
}
