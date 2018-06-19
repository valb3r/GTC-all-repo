package com.gtc.tradinggateway.service.therocktrading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.model.gateway.data.OrderStatus;
import com.gtc.tradinggateway.service.dto.OrderCreatedDto;
import com.gtc.tradinggateway.service.therocktrading.TheRockTradingRestService;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class TheRockTradingOrderDto {

    private static final String BUY = "buy";

    private String id;

    @JsonProperty("fund_id")
    private String pair;

    private String side;
    private BigDecimal price;
    private BigDecimal amount;
    private String status;

    public OrderDto mapTo() {
        return OrderDto.builder()
                .orderId(new TheRockTradingRestService.SymbolAndId(pair, id).toString())
                .size(BUY.equalsIgnoreCase(side) ? amount : amount.negate())
                .price(price)
                .statusString(status)
                .status(MAPPER.getOrDefault(status, OrderStatus.UNMAPPED))
                .build();
    }

    public OrderCreatedDto mapToCreate() {
        return OrderCreatedDto.builder()
                .assignedId(new TheRockTradingRestService.SymbolAndId(pair, id).toString())
                .build();
    }

    private static final Map<String, OrderStatus> MAPPER = ImmutableMap.<String, OrderStatus>builder()
            .put("active", OrderStatus.NEW)
            .put("conditional", OrderStatus.UNMAPPED)
            .put("executed", OrderStatus.FILLED)
            .put("deleted", OrderStatus.UNMAPPED)
            .build();
}


