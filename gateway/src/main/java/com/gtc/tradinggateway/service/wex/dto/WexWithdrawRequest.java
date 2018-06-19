package com.gtc.tradinggateway.service.wex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 05.03.18.
 */
@Getter
public class WexWithdrawRequest extends BaseWexRequest {

    @JsonProperty("coinName")
    private final String coinName;

    private final BigDecimal amount;
    private final String dest;

    public WexWithdrawRequest(int nonce, String method, String coinName, BigDecimal amount, String dest) {
        super(nonce, method);
        this.coinName = coinName;
        this.amount = amount;
        this.dest = dest;
    }
}
