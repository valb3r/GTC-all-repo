package com.gtc.provider.config;

import lombok.experimental.UtilityClass;

/**
 * Created by Valentyn Berezin on 29.12.17.
 */
@UtilityClass
public class Const {

    static final String CONF_ROOT = "app";
    public static final String CONF_ROOT_CHILD = CONF_ROOT + ".";
    public static final String CONF_ROOT_SCHEDULE_CHILD = CONF_ROOT_CHILD + "schedule.";

    public static final String BITFINEX = "bitfinex";
    public static final String BITSTAMP = "bitstamp";
    public static final String THE_ROCK_TRADING = "therocktrading";
    public static final String ZB = "zb";
    public static final String HITBTC = "hitbtc";
    public static final String POLONIEX = "poloniex";
    public static final String GDAX = "gdax";
    public static final String BINANCE = "binance";
    public static final String MOCK = "mock";
    public static final String OKEX = "okex";
    public static final String HUOBI = "huobi";
    public static final String EXX = "exx";
    public static final String WEX = "wex";
    public static final String CLIENTS = "clients";

    @UtilityClass
    public class Rest {

        public static final String STAT = "/stat";
        public static final String CLIENT = "/client";
    }

    @UtilityClass
    public class Write {

        public static final String WRITE = "write";
    }

    @UtilityClass
    public class Ws {
        public static final String ORDER_TICKER_QUEUE = "/market";
    }
}
