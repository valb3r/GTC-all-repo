package com.gtc.provider.clients.wex.dto;

import lombok.Data;

/**
 * Created by mikro on 17.01.2018.
 */
@Data
public class WexTickerResponse {

    private String channel;
    private String event;
    private String data;

}
