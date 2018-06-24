package com.gtc.tradinggateway.service.okex.dto;

/**
 * Created by Valentyn Berezin on 23.06.18.
 */
public interface IsResultOk {

    boolean isResult();
    long getErrorCode();
}
