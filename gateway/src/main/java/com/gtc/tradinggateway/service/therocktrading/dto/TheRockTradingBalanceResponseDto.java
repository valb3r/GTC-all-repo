package com.gtc.tradinggateway.service.therocktrading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TheRockTradingBalanceResponseDto {

    private List<BalanceItem> balances;

    @Data
    public static class BalanceItem {

        private String currency;

        @JsonProperty("trading_balance")
        private BigDecimal balance;
    }
}
