package com.gtc.opportunity.trader.service.nnopportunity.creation.fitter;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 31.08.18.
 */
@Data
public class FeeFitted {

    private final BigDecimal buyPrice;
    private final BigDecimal sellPrice;
    private final BigDecimal buyAmount;
    private final BigDecimal sellAmount;

    private final BigDecimal amount;
    private final BigDecimal profitFrom;
    private final BigDecimal profitTo;
}
