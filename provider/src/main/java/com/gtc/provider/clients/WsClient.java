package com.gtc.provider.clients;

import java.util.Map;

/**
 * Created by Valentyn Berezin on 28.12.17.
 */
public interface WsClient {

    String name();
    void connect(Map<String, String> headers);
    boolean isDisconnected();
    long connectedAtTimestamp();

    MarketDto market();
}
