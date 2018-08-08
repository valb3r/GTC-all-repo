package com.gtc.opportunity.trader.service.xoopportunity.replenishment.precision.optaplan;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 26.03.18.
 */
@Data
public class XoReplenishPrice {

    private final BigDecimal lossFrom;
    private final BigDecimal lossTo;
    private final BigDecimal targetBuyPrice;
    private final BigDecimal targetSellPrice;
}
