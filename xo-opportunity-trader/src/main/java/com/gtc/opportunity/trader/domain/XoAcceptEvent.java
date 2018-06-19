package com.gtc.opportunity.trader.domain;

/**
 * Created by Valentyn Berezin on 02.03.18.
 */
public enum XoAcceptEvent {

    TRADE_ISSUE,
    TRADE_ACK,
    TRADE_DONE,
    ERROR;

    public static final String MSG_ID = "messageId";
    public static final String ORDER_ID = "orderId";
}
