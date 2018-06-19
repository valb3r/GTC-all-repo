package com.gtc.provider.clients.common.pusher.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by Valentyn Berezin on 09.01.18.
 */
@Data
public class PusherSubscribe {

    private String event = "pusher:subscribe";
    private final ToChannel data;

    @Data
    @AllArgsConstructor
    public static class ToChannel {

        private String channel;
    }
}
