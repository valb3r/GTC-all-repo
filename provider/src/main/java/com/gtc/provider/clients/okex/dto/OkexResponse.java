package com.gtc.provider.clients.okex.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Created by mikro on 12.01.2018.
 */
@Data
public class OkexResponse {

    private String channel;

    private JsonNode data;

    public String getType() {
        String[] channelList = channel.split("_");
        return channelList[channelList.length - 1];
    }

    public String getSymbol() {
        String[] channelList = channel.split("_");
        if (channelList.length == 1) {
            return null;
        }
        return channelList[channelList.length - 3] + "_" + channelList[channelList.length - 2];
    }

}
