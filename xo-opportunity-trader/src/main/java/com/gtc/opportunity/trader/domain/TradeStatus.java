package com.gtc.opportunity.trader.domain;

/**
 * Created by Valentyn Berezin on 25.02.18.
 */
public enum TradeStatus {

    UNKNOWN,
    DEPENDS_ON,
    ERR_OPEN,
    NEED_RETRY,
    OPENED,
    GEN_ERR,
    CANCELLED,
    DONE_MAN,
    CLOSED
}
