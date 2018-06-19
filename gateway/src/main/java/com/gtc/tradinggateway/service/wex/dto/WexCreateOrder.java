package com.gtc.tradinggateway.service.wex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 04.03.18.
 */
@Getter
@Setter
public class WexCreateOrder extends BaseWexRequest {

    private final String pair;
    private final String type;

    @JsonProperty("rate")
    private final BigDecimal price;

    private final BigDecimal amount;

    public WexCreateOrder(int nonce, String method, String pair, String type, BigDecimal price, BigDecimal amount) {
        super(nonce, method);
        this.pair = pair;
        this.type = type;
        this.price = price;
        this.amount = amount;
    }
}
