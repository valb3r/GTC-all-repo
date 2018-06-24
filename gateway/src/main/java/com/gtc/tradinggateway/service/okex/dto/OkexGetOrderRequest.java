package com.gtc.tradinggateway.service.okex.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 23.06.18.
 */
@Data
public class OkexGetOrderRequest {

    private final String orderId;
    private final String symbol;
}
