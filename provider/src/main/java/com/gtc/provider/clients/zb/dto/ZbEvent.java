package com.gtc.provider.clients.zb.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Data
public class ZbEvent {

    private String channel;
    private Ticker ticker;

    private List<Double[]> asks = new ArrayList<>();
    private List<Double[]> bids = new ArrayList<>();

    @Data
    public static class Ticker {
        private String vol;
        private String high;
        private String last;
        private String buy;
        private String sell;
    }
}
