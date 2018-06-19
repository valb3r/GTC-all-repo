package com.gtc.opportunity.trader.domain;

/**
 * Created by Valentyn Berezin on 28.02.18.
 */
public enum TradeEvent {

    ACK,
    RETRY,
    TIMEOUT,
    TRANSIENT_ERR,
    ERROR,
    CANCELLED,
    DONE;

    public static final String DATA = "data";
    public static final String MSG_ID = "messageId";
    public static final String AMOUNT = "amount";
    public static final String PRICE = "price";
    public static final String STATUS = "status";
    public static final String NATIVE_STATUS = "nativeStatus";
}
