package com.gtc.tradinggateway.service.binance.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Created by mikro on 28.01.2018.
 */
@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BinanceWithdrawalRequestDto extends BinanceRequestDto {

    private final String asset;
    private final BigDecimal amount;
    private final String address;
}
