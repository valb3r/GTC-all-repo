package com.gtc.tradinggateway.service.therocktrading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
public class TheRockTradingWithdrawRequestDto {

    @JsonProperty("destination_address")
    private final String address;

    private final String currency;
    private final BigDecimal amount;
}
