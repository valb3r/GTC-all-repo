package com.gtc.tradinggateway.service.bitfinex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by mikro on 20.02.2018.
 */
@Data
public class BitfinexOrderDto {

    private String id;
    private String symbol;
    private BigDecimal price;
    private String side;

    @JsonProperty("is_live")
    private boolean isActive;

    @JsonProperty("is_cancelled")
    private boolean isCancelled;

    @JsonProperty("remaining_amount")
    private BigDecimal amount;

    @JsonProperty("executed_amount")
    private BigDecimal executedAmount;
}
