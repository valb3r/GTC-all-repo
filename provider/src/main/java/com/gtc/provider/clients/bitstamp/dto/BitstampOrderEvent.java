package com.gtc.provider.clients.bitstamp.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Valentyn Berezin on 09.01.18.
 */
@Data
public class BitstampOrderEvent {

    private String event;
    private String channel;
    private String data;

    @Data
    public static class InnerData {

        private List<String[]> bids = new ArrayList<>();
        private List<String[]> asks = new ArrayList<>();
    }
}
