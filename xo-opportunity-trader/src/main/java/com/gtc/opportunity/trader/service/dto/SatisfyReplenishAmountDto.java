package com.gtc.opportunity.trader.service.dto;

import com.gtc.opportunity.trader.domain.ClientConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Buy From-> Sell To. In terms of actions on our side (you do sell).
 */
@Data
@AllArgsConstructor
public class SatisfyReplenishAmountDto {

    private BigDecimal sellPrice; // you should sell with this price
    private BigDecimal buyPrice; // you should buy with this price

    private double maxSellAmount;
    private double maxBuyAmount;

    private ClientConfig from;
    private ClientConfig to;
}
