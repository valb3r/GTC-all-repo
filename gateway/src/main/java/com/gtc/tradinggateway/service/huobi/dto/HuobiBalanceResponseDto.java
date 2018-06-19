package com.gtc.tradinggateway.service.huobi.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class HuobiBalanceResponseDto {

    private Balance data;

    @Data
    public static class Balance {

        private List<BalanceItem> list;
    }

    @Data
    public static class BalanceItem {

        private String currency;
        private String type;

        @JsonProperty("balance")
        private BigDecimal amount;

        public boolean isTrade() {
            return "trade".equalsIgnoreCase(type);
        }
    }
}
