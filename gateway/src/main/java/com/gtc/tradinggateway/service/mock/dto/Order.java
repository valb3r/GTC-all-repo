package com.gtc.tradinggateway.service.mock.dto;

import com.google.common.collect.ImmutableMap;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.model.gateway.data.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by Valentyn Berezin on 08.03.18.
 */
@Getter
@Builder
@RequiredArgsConstructor
public class Order {

    private static final Map<Status, OrderStatus> MAPPER = ImmutableMap.<Status, OrderStatus>builder()
            .put(Status.OPEN, OrderStatus.NEW)
            .put(Status.DONE, OrderStatus.FILLED)
            .put(Status.CANCELLED, OrderStatus.CANCELED)
            .build();

    private final String id;
    private final String clientId;
    private final BigDecimal price;
    private final BigDecimal amount;
    private final Status status;
    private final long timestamp = System.currentTimeMillis();

    public enum Status {

        OPEN,
        CANCELLED,
        DONE
    }

    public OrderDto mapTo () {
        return OrderDto.builder()
                .orderId(id)
                .size(amount)
                .price(price)
                .status(MAPPER.getOrDefault(status, OrderStatus.UNMAPPED))
                .statusString(status.name())
                .build();
    }
}
