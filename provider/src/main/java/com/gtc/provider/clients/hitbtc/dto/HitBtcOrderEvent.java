package com.gtc.provider.clients.hitbtc.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Data
public class HitBtcOrderEvent {

    private String method;
    private Params params;

    @Data
    public static class Params {

        private String symbol;
        private List<Order> ask = new ArrayList<>();
        private List<Order> bid = new ArrayList<>();

        @Data
        public static class Order {

            private String price;
            private String size;
        }
    }
}
