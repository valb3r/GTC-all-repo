package com.gtc.opportunity.trader.domain;

import lombok.experimental.UtilityClass;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@UtilityClass
public final class Const {

    public static final String CLIENT_NAME = "client_name";

    @UtilityClass
    public final class InternalMessaging {

        public static final String MSG_ID = "messageId";
        public static final String ORDER_ID = "orderId";
    }
}
