package com.gtc.tradinggateway.service.binance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by mikro on 31.01.2018.
 */
@Data
public class BinanceBalanceDto {

    private BinanceBalanceAsset[] balances;

    @Data
    public static class BinanceBalanceAsset {

        @JsonProperty("asset")
        private String code;

        @JsonProperty("free")
        private BigDecimal amount;
    }
}
