package com.gtc.tradinggateway.service.therocktrading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TheRockTradingCreateRequestDto {

    @JsonProperty("fund_id")
    private final String pair;

    private final String side;
    private final String amount;
    private final String price;
}
