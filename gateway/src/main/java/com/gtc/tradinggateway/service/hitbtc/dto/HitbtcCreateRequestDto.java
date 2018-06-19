package com.gtc.tradinggateway.service.hitbtc.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Created by mikro on 14.02.2018.
 */
@Data
public class HitbtcCreateRequestDto {

    private String method = "newOrder";

    private OrderBody params;

    @Data
    @RequiredArgsConstructor
    public static class OrderBody {

        private final String clientOrderId;
        private final String type = "limit";
        private final String symbol;
        private final String side;
        private final BigDecimal price;
        private final BigDecimal quantity;
    }

    public HitbtcCreateRequestDto(String id, String symbol, String side, BigDecimal price, BigDecimal quantity) {
        params = new OrderBody(id, symbol, side, price, quantity);
    }
}
