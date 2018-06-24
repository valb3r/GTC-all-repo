package com.gtc.tradinggateway.service.okex.dto;

import com.google.common.base.Strings;
import lombok.Data;

/**
 * Created by Valentyn Berezin on 24.06.18.
 */
@Data
public class OkexCancelOrderResponse implements IsResultOk {

    private String orderId;
    private boolean result;
    private long errorCode;

    @Override
    public boolean isResult() {
        return result && !Strings.isNullOrEmpty(orderId);
    }
}
