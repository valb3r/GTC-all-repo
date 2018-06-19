package com.gtc.provider.clients.bitfinex.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 29.12.17.
 */
@Data
public class SubscribedEvent {

    private String event;
    private String channel;
    private long chanId;
    private String symbol;
    private String prec;
    private String pair;
}
