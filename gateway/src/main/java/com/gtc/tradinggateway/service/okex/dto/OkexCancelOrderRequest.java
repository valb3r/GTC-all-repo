package com.gtc.tradinggateway.service.okex.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 24.06.18.
 */
@Data
public class OkexCancelOrderRequest {

    private final String orderId;
    private final String symbol;
}
