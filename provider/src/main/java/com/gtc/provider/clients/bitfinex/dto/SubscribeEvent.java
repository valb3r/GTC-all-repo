package com.gtc.provider.clients.bitfinex.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 29.12.17.
 */
@Data
public class SubscribeEvent {

    private final String symbol;
    private final String channel;

    private String event = "subscribe";
    private String prec = "R0";
    private int len = 100; // 100 or 25 are supported

    public SubscribeEvent(String channel, String symbol) {
        this.channel = channel;
        this.symbol = symbol;
    }
}
