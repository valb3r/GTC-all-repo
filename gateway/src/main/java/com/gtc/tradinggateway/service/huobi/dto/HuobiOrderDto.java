package com.gtc.tradinggateway.service.huobi.dto;

import com.google.common.collect.ImmutableMap;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.model.gateway.data.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by mikro on 01.04.2018.
 */
@Data
public class HuobiOrderDto {

    private static final Map<String, OrderStatus> MAPPER = ImmutableMap.<String, OrderStatus>builder()
            .put("pre-submitted", OrderStatus.NEW)
            .put("submitting", OrderStatus.NEW)
            .put("submitted", OrderStatus.NEW)
            .put("partial-filled", OrderStatus.PARTIALLY_FILLED)
            .put("filled", OrderStatus.FILLED)
            .put("canceled", OrderStatus.CANCELED)
            .build();

    private Number id;
    private String symbol;
    private BigDecimal price;
    private BigDecimal amount;
    private String state;

    public OrderDto mapTo() {
        return OrderDto.builder()
                .orderId(symbol + "." + id.toString())
                .size(amount)
                .price(price)
                .status(MAPPER.getOrDefault(state, OrderStatus.UNMAPPED))
                .build();
    }
}
