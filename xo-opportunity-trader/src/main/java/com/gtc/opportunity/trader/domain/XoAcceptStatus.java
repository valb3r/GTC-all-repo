package com.gtc.opportunity.trader.domain;

/**
 * Created by Valentyn Berezin on 25.02.18.
 */
public enum XoAcceptStatus {

    UNCONFIRMED,
    TRADE_ISSUE,
    ERROR,
    TRANSIENT_ISSUE,
    ACK_PART,
    ACK_BOTH,
    DONE_PART,
    DONE_BOTH,
    REPLENISH,
    REPL_ACK_PART,
    REPL_ACK_BOTH,
    REPL_DONE_PART,
    REPL_DONE_BOTH,
    REPL_TRADE_ISSUE,
    DONE;
}
