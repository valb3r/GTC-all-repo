package com.gtc.tradinggateway.service.bitfinex.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Created by mikro on 18.02.2018.
 */
@Getter
@RequiredArgsConstructor
public class BitfinexRequestDto {

    protected final String request;

    // FIXME: most probably we need same nonce logic like there is in WexRestService
    @Setter
    protected String nonce = String.valueOf(LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond());
}
