package com.gtc.tradinggateway.service.bitfinex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by mikro on 21.02.2018.
 */
@Data
public class BitfinexCreateOrderWsDto {

    private String type = "EXCHANGE LIMIT";

    @JsonProperty("cid")
    private long id;
    private String symbol;
    private String amount;
    private String price;

    public BitfinexCreateOrderWsDto(String id, String symbol, BigDecimal amount, BigDecimal price) {
        this.id = Long.parseLong(id);
        this.symbol = "t" + symbol;
        this.amount = amount.toString();
        this.price = price.toString();
    }
}
