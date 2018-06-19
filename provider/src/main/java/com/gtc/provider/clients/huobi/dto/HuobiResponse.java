package com.gtc.provider.clients.huobi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Created by Valentyn Berezin on 13.01.18.
 */
@Data
public class HuobiResponse {

    @JsonProperty("ch")
    private String channel;

    private Tick tick;

    @Data
    public static class Tick {

        private List<double[]> bids;
        private List<double[]> asks;
        private List<InnerData> data;

        @Data
        public static class InnerData {

            double price;
        }
    }
}
