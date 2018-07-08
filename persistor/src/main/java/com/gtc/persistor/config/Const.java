package com.gtc.persistor.config;

import lombok.experimental.UtilityClass;

/**
 * Created by Valentyn Berezin on 01.07.18.
 */
@UtilityClass
public class Const {

    static final String CONF_ROOT = "app";
    public static final String CONF_ROOT_CHILD = CONF_ROOT + ".";

    @UtilityClass
    public class Ws {

        static final String CHILD = CONF_ROOT_CHILD + "ws.";
        public static final String PROVIDER = "provider";
        public static final String WS_RECONNECT_S = "#{${app.schedule.wsReconnectS} * 1000}";
    }

    @UtilityClass
    public class Persist {

        static final String PERSIST_CFG = CONF_ROOT_CHILD + "persist";
        public static final String PERSIST_S = "#{${app.schedule.persistS} * 1000}";
    }
}
