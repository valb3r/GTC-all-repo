package com.gtc.tradinggateway.service;

import com.gtc.tradinggateway.meta.TradingCurrency;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 16.01.18.
 */
public interface Withdraw extends ClientNamed {

    void withdraw(TradingCurrency currency, BigDecimal amount, String destination);
}
