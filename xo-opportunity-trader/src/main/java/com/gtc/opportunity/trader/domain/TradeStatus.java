package com.gtc.opportunity.trader.domain;

/**
 * Created by Valentyn Berezin on 25.02.18.
 */
public enum TradeStatus {

    UNKNOWN,
    ERR_OPEN,
    NEED_RETRY,
    OPENED,
    GEN_ERR,
    CANCELLED,
    CLOSED
}
