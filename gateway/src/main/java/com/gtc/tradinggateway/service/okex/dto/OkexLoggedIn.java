package com.gtc.tradinggateway.service.okex.dto;

import lombok.Data;
import lombok.experimental.Delegate;

/**
 * Created by Valentyn Berezin on 24.06.18.
 */
@Data
public class OkexLoggedIn implements IsResultOk {

    private String channel;

    @Delegate
    private DataEntry data;

    @Data
    public static class DataEntry implements IsResultOk {
        private boolean result;
        private long errorCode;
    }
}
