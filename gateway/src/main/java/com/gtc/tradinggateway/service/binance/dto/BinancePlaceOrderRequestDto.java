package com.gtc.tradinggateway.service.binance.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Created by mikro on 01.02.2018.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BinancePlaceOrderRequestDto extends BinanceRequestDto {

    private String symbol;

    private String side;

    private String type = "LIMIT";

    private BigDecimal quantity;

    private BigDecimal price;

    private String timeInForce = "GTC";

    public BinancePlaceOrderRequestDto (String symbol, String side, BigDecimal amount, BigDecimal price) {
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.quantity = amount;
    }
}
