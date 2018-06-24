package com.gtc.tradinggateway.service.okex.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Valentyn Berezin on 23.06.18.
 */
@Data
public class OkexBalanceResponse implements IsResultOk {

    private Info info = new Info();
    private boolean result;
    private long errorCode;

    @Data
    public static class Info {

        private Funds funds = new Funds();

        @Data
        public static class Funds {

            private Map<String, BigDecimal> free = new HashMap<>();
        }
    }
}
