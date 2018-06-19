package com.gtc.provider.clients.zb.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Data
public class ZbSubscribe {

    private final String event;
    private final String channel;
}
