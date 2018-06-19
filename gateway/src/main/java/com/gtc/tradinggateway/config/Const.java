package com.gtc.tradinggateway.config;

import lombok.experimental.UtilityClass;

/**
 * Created by Valentyn Berezin on 16.01.18.
 */
@UtilityClass
public class Const {

    static final String CONF_ROOT = "app";
    public static final String CONF_ROOT_CHILD = CONF_ROOT + ".";

    @UtilityClass
    public class Schedule {

        public static final String SCHEDULE = "schedule";
        public static final String CONF_ROOT_SCHEDULE_CHILD = CONF_ROOT_CHILD + SCHEDULE + ".";
    }

    @UtilityClass
    public class Clients {

        public static final String CLIENTS = "clients";
        public static final String GDAX = "gdax";
        public static final String WEX = "wex";
        public static final String BINANCE = "binance";
        public static final String MOCK = "mock";
        public static final String HITBTC = "hitbtc";
        public static final String HUOBI = "huobi";
        public static final String BITFINEX = "bitfinex";
        public static final String THEROCKTRADING = "therocktrading";
    }

    @UtilityClass
    public class Ws {

        public static final String WS_API = "ws";
    }

    public static final String STATISTICS = "statistics";
}
