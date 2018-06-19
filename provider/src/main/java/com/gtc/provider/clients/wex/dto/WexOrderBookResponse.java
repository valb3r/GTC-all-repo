package com.gtc.provider.clients.wex.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mikro on 17.01.2018.
 */
@Data
public class WexOrderBookResponse {

    private String channel;
    private String event;
    private String data;

    @Data
    public static class WexOrderBookItem {

        private List<double[]> bid = new ArrayList();
        private List<double[]> ask = new ArrayList();

    }

}
