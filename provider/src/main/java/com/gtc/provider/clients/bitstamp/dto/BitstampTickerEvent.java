package com.gtc.provider.clients.bitstamp.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 09.01.18.
 */
@Data
public class BitstampTickerEvent {

    private String event;
    private String channel;
    private String data;

    @Data
    public static class InnerData {

        double amount;
        double price;
        long id;
    }
}
