package com.gtc.provider.clients.okex.dto;

import lombok.Data;

/**
 * Created by mikro on 12.01.2018.
 */
@Data
public class OkexRequest {

    private String event;

    private String channel;

    public OkexRequest(String event, String channel) {
        this.event = event;
        this.channel = channel;
    }

}
