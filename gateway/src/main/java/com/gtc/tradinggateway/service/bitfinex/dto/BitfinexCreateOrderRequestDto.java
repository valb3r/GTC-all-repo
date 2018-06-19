package com.gtc.tradinggateway.service.bitfinex.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class BitfinexCreateOrderRequestDto extends BitfinexRequestDto {

    private final String type = "exchange limit";
    private final String exchange = "bitfinex";

    private final String symbol;
    private final String side;
    private final String amount;
    private final String price;

    public BitfinexCreateOrderRequestDto(String request, String symbol, String side, BigDecimal amount, BigDecimal price) {
        super(request);
        this.symbol = symbol;
        this.side = side;
        this.amount = amount.toString();
        this.price = price.toString();
    }
}
