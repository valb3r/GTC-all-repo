package com.gtc.tradinggateway.service.okex.dto;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.model.gateway.data.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 23.06.18.
 */
@Data
public class OkexGetOrderResponse implements IsResultOk {

    private static final Map<Integer, OrderStatus> MAPPER = ImmutableMap.<Integer, OrderStatus>builder()
            .put(0, OrderStatus.NEW)
            .put(1, OrderStatus.PARTIALLY_FILLED)
            .put(2, OrderStatus.FILLED)
            .put(-1, OrderStatus.CANCELED)
            .build();

    private List<Order> orders = new ArrayList<>();
    private boolean result;
    private long errorCode;

    public static OrderDto mapTo(Order order) {
        return OrderDto.builder()
                .orderId(order.getSymbol() + "." + order.getOrderId())
                .size(order.getDealAmount())
                .price(order.getPrice())
                .status(MAPPER.get(order.getStatus()))
                .statusString(String.valueOf(order.getStatus()))
                .build();
    }

    public Optional<OrderDto> mapTo() {
        Order value = Iterables.getFirst(orders, null);
        if (null == value) {
            return Optional.empty();
        }

        return Optional.of(mapTo(value));
    }

    public List<OrderDto> mapAll() {
        return orders.stream().map(OkexGetOrderResponse::mapTo).collect(Collectors.toList());
    }

    @Data
    public static class Order {

        private BigDecimal amount;
        private BigDecimal avgPrice;
        private BigDecimal dealAmount;
        private String orderId;
        private BigDecimal price;
        private int status;
        private String symbol;
        private String type;
    }
}
