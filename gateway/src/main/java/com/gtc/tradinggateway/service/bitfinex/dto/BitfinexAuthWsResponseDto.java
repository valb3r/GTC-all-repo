package com.gtc.tradinggateway.service.bitfinex.dto;

import lombok.Data;

/**
 * Created by mikro on 21.02.2018.
 */
@Data
public class BitfinexAuthWsResponseDto {

    private String event;
    private String status;
}
