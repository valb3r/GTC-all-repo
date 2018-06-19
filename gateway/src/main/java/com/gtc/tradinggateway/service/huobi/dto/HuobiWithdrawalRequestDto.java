package com.gtc.tradinggateway.service.huobi.dto;

import lombok.Data;

/**
 * Created by mikro on 01.04.2018.
 */
@Data
public class HuobiWithdrawalRequestDto {

    private final String address;
    private final String amount;
    private final String currency;
}
