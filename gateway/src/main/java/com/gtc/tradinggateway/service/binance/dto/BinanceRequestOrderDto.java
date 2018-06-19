package com.gtc.tradinggateway.service.binance.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by mikro on 25.01.2018.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BinanceRequestOrderDto extends BinanceRequestDto {

    private String symbol;
    private String orderId;

    public BinanceRequestOrderDto(String id) {
        String[] parsedId = id.split("\\.");
        symbol = parsedId[0];
        orderId = parsedId[1];
    }
}
