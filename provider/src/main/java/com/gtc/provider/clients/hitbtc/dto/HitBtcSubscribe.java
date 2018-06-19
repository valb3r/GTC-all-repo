package com.gtc.provider.clients.hitbtc.dto;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Data
public class HitBtcSubscribe {

    private static final AtomicLong counter = new AtomicLong(0);

    private long id = counter.getAndIncrement();
    private final String method;
    private final Params params;

    public HitBtcSubscribe(String method, String symbol) {
        this.method = method;
        this.params = new Params(symbol);
    }

    @Data
    public static class Params {
        private final String symbol;
    }

}
