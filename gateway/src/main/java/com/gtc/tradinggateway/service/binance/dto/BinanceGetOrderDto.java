package com.gtc.tradinggateway.service.binance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.model.gateway.data.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;


/**
 * Created by mikro on 24.01.2018.
 */
@Data
public class BinanceGetOrderDto {

    private static final Map<String, OrderStatus> MAPPER = ImmutableMap.<String, OrderStatus>builder()
            .put("NEW", OrderStatus.NEW)
            .put("PARTIALLY_FILLED", OrderStatus.PARTIALLY_FILLED)
            .put("FILLED", OrderStatus.FILLED)
            .put("CANCELED", OrderStatus.CANCELED)
            .put("REJECTED", OrderStatus.REJECTED)
            .put("EXPIRED", OrderStatus.EXPIRED)
            .build();

    @JsonProperty("orderId")
    private String id;

    @JsonProperty("symbol")
    private String pair;

    @JsonProperty("origQty")
    private BigDecimal originalAmount = BigDecimal.ZERO;

    private BigDecimal executedQty = BigDecimal.ZERO;

    @JsonProperty("quantity")
    private BigDecimal currentAmount;

    private BigDecimal price;

    private String status;

    public OrderDto mapTo() {
        return OrderDto.builder()
                .orderId(pair + "." + id)
                .size(originalAmount.subtract(executedQty))
                .price(price)
                .status(MAPPER.getOrDefault(status, OrderStatus.UNMAPPED))
                .statusString(status)
                .build();
    }
}
