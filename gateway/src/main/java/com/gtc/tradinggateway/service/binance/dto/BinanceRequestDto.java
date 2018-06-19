package com.gtc.tradinggateway.service.binance.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by mikro on 28.01.2018.
 */
@Getter
@Setter
public class BinanceRequestDto {

    protected long timestamp = System.currentTimeMillis();
    protected int recvWindow = 5000;
}
