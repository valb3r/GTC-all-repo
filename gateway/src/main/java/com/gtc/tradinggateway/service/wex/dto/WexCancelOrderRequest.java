package com.gtc.tradinggateway.service.wex.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by Valentyn Berezin on 05.03.18.
 */
@Getter
@Setter
public class WexCancelOrderRequest extends BaseWexRequest {

    private final long orderId;

    public WexCancelOrderRequest(int nonce, String method, long orderId) {
        super(nonce, method);
        this.orderId = orderId;
    }
}
