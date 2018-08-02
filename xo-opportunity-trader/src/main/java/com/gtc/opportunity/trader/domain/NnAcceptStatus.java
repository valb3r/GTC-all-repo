package com.gtc.opportunity.trader.domain;

/**
 * Created by Valentyn Berezin on 02.08.18.
 */
public enum NnAcceptStatus {

    UNCONFIRMED,
    TRADE_ISSUE,
    ERROR,
    ACK_PART,
    ACK_BOTH,
    DONE_PART,
    DONE;
}
