package com.gtc.provider.clients.huobi.dto;

import lombok.Data;

import java.util.UUID;

/**
 * Created by mikro on 13.01.2018.
 */
@Data
public class HuobiSubscribeRequest {

    private String sub;
    private String id;

    public HuobiSubscribeRequest(String sub) {
        this.sub = sub;
        this.id = UUID.randomUUID().toString();
    }

}
