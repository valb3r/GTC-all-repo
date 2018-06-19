package com.gtc.tradinggateway.service.bitfinex.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Created by mikro on 21.02.2018.
 */
@Data
@RequiredArgsConstructor
public class BitfinexAuthWsRequestDto {

    private final String event = "auth";
    private final String apiKey;
    private final String authSig;
    private final String authPayload;
    private final String authNonce;
}
