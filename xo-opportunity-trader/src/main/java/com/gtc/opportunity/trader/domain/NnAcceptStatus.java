package com.gtc.opportunity.trader.domain;

/**
 * Created by Valentyn Berezin on 02.08.18.
 */
public enum NnAcceptStatus {

    MASTER_UNKNOWN,
    MASTER_OPENED,
    MASTER_ISSUE,
    PENDING_SLAVE,
    SLAVE_UNKNOWN,
    SLAVE_OPENED,
    SLAVE_ISSUE,
    ERROR,
    ABORTED,
    DONE
}
