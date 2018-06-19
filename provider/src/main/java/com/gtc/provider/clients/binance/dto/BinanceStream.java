package com.gtc.provider.clients.binance.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Created by mikro on 09.01.2018.
 */
@Data
public class BinanceStream {

    private String stream;

    private JsonNode data;

    public String getType() {
        return stream.split("@")[1];
    }

}
