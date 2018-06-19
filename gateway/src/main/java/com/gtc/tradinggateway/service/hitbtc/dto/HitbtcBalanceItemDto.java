package com.gtc.tradinggateway.service.hitbtc.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by mikro on 13.02.2018.
 */
@Data
public class HitbtcBalanceItemDto {

    private String currency;
    private BigDecimal available;
}
