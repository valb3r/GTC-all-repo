package com.gtc.opportunity.trader.service.compute;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 04.09.18.
 */
@Data
@AllArgsConstructor
public class BalanceChange {

    private final BigDecimal from;
    private final BigDecimal to;

    BalanceChange() {
        from = BigDecimal.ZERO;
        to = BigDecimal.ZERO;
    }

    BalanceChange add(BalanceChange other) {
        return new BalanceChange(from.add(other.from), to.add(other.to));
    }
}
