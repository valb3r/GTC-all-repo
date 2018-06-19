package com.gtc.tradinggateway.service.huobi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Created by mikro on 01.04.2018.
 */
@Getter
@RequiredArgsConstructor
public class HuobiCreateRequestDto {

    private final String source = "api";

    @JsonProperty("account-id")
    private final long accountId;

    private final String type;
    private final BigDecimal amount;
    private final BigDecimal price;
    private final String symbol;
}
