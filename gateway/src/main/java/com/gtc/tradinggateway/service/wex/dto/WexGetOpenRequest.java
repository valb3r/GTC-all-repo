package com.gtc.tradinggateway.service.wex.dto;

/**
 * Created by Valentyn Berezin on 05.03.18.
 */
public class WexGetOpenRequest extends BaseWexRequest {

    public WexGetOpenRequest(int nonce, String method) {
        super(nonce, method);
    }
}
