package com.gtc.opportunity.trader.service.command;

import lombok.experimental.UtilityClass;

/**
 * Created by Valentyn Berezin on 28.02.18.
 */
@UtilityClass
public class Const {

    public static final String ACCOUNT_REQ_QUEUE = "${app.jms.queue.out.account}";
    public static final String CREATE_REQ_QUEUE = "${app.jms.queue.out.create}";
    public static final String MANAGE_REQ_QUEUE = "${app.jms.queue.out.manage}";

    public static final String ACCOUNT_RESP_QUEUE = "${app.jms.queue.in.account}";
    public static final String CREATE_RESP_QUEUE = "${app.jms.queue.in.create}";
    public static final String MANAGE_RESP_QUEUE = "${app.jms.queue.in.manage}";
}
