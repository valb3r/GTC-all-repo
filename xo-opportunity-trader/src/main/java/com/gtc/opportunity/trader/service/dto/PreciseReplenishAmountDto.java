package com.gtc.opportunity.trader.service.dto;

import com.gtc.opportunity.trader.domain.ClientConfig;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Sell to, buy from.
 */
@Data
public class PreciseReplenishAmountDto {

    private final BigDecimal sellPrice;
    private final BigDecimal sellAmount;
    private final BigDecimal buyPrice;
    private final BigDecimal buyAmount;
    private final BigDecimal lossAmount;

    private final ClientConfig from;
    private final ClientConfig to;
}
