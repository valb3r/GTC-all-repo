package com.gtc.provider.clients.binance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Created by mikro on 09.01.2018.
 */
@Data
public class BinanceOrderBookResponse {

    @JsonProperty("s")
    private String symbol;

    @JsonProperty("b")
    private JsonNode bids;

    @JsonProperty("a")
    private JsonNode asks;

}
