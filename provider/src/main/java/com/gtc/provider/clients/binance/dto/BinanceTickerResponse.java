package com.gtc.provider.clients.binance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Created by mikro on 09.01.2018.
 */
@Data
public class BinanceTickerResponse {

    @JsonProperty("s")
    private String symbol;

    @JsonProperty("o")
    private double price;

}
