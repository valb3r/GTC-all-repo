package com.gtc.tradinggateway.service.bitfinex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Created by mikro on 20.02.2018.
 */
@Getter
public class BitfinexWithdrawRequestDto extends BitfinexRequestDto {

    @JsonProperty("withdraw_type")
    private final String symbol;

    private final String walletselected = "exchange";

    private final String amount;

    private final String address;

    public BitfinexWithdrawRequestDto(String request, String symbol, BigDecimal amount, String address) {
        super(request);
        this.symbol = symbol;
        this.amount = String.valueOf(amount);
        this.address = address;
    }
}
