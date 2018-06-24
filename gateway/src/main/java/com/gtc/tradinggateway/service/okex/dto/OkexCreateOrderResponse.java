package com.gtc.tradinggateway.service.okex.dto;

import com.gtc.tradinggateway.service.dto.OrderCreatedDto;
import lombok.Data;

/**
 * Created by Valentyn Berezin on 24.06.18.
 */
@Data
public class OkexCreateOrderResponse implements IsResultOk {

    private boolean result;
    private long errorCode;
    private long orderId;

    public OrderCreatedDto mapToCreated(String origId, String symbol) {
        return OrderCreatedDto.builder()
                .assignedId(symbol + "." + String.valueOf(orderId))
                .requestedId(origId)
                .isExecuted(false)
                .build();
    }
}
