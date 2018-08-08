package com.gtc.opportunity.trader.service.xoopportunity.replenishment.dto;

import com.gtc.opportunity.trader.domain.ClientConfig;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 02.04.18.
 */
@Data
public class Replenish {

    private final BigDecimal price;
    private final BigDecimal amount;
    private final ClientConfig cfg;
    private final boolean isSell;
}
