package com.gtc.provider.clients.hitbtc.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Data
public class HitBtcTickerEvent {

    private String method;
    private Params params;

    @Data
    public static class Params {

        private String last;
        private String symbol;
    }
}
