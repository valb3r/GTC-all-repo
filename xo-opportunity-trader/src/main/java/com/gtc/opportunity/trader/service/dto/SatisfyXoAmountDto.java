package com.gtc.opportunity.trader.service.dto;

import com.gtc.opportunity.trader.domain.ClientConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Sell From-> Buy To. In terms of actions on our side (you do sell).
 */
@Data
@AllArgsConstructor
public class SatisfyXoAmountDto {

    private double sellPrice; // you should sell with this price
    private double buyPrice; // you should buy with this price
    private double minSellAmount;
    private double maxSellAmount;
    private double minBuyAmount;
    private double maxBuyAmount;
    private double profitPct;

    private ClientConfig from;
    private ClientConfig to;
}
