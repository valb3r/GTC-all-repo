package com.gtc.provider.clients.common.pusher.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 12.01.18.
 */
@Data
public class PusherEvent {

    private String channel;
    private String data;
    private String event;
}
