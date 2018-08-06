package com.gtc.opportunity.trader.config;

import lombok.experimental.UtilityClass;

/**
 * Created by Valentyn Berezin on 10.01.18.
 */
@UtilityClass
public final class Const {

    static final String CONF_ROOT = "app";
    public static final String CONF_ROOT_CHILD = CONF_ROOT + ".";

    @UtilityClass
    public static final class Cache {

        public static final String CACHE = "cache";
    }

    @UtilityClass
    public static final class Scheduled {

        public static final String PUSH_STAT = "#{${app.schedule.pushStatsS} * 1000}";
    }

    @UtilityClass
    public static final class Common {

        public static final String XO_OPPORTUNITY_PREFIX = "XOM-";
        public static final String NN_OPPORTUNITY_PREFIX = "NNO-";
    }

    @UtilityClass
    public static final class Ws {

        public static final String WS = "ws";
        public static final String WS_RECONNECT_S = "#{${app.schedule.wsReconnectS} * 1000}";
    }

    @UtilityClass
    public static final class Opportunity {

        public static final String OPPORTUNITY_CONF = "opportunities";
    }

    @UtilityClass
    public static final class SpringProfiles {

        public static final String TEST = "test";
    }
}
