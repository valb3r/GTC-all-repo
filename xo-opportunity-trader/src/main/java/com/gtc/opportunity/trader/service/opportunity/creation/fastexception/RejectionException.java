package com.gtc.opportunity.trader.service.opportunity.creation.fastexception;

import lombok.Getter;

/**
 * Created by Valentyn Berezin on 01.04.18.
 */
@Getter
public class RejectionException extends IllegalStateException {

    private final Reason reason;
    private final Double threshold;
    private final Double value;

    public RejectionException(Reason reason) {
        super(reason.getMsg());
        this.reason = reason;
        this.threshold = null;
        this.value = null;
    }

    public RejectionException(Reason reason, Double value) {
        super(reason.getMsg());
        this.reason = reason;
        this.value = value;
        this.threshold = null;
    }

    public RejectionException(Reason reason, Double value, Double threshold) {
        super(reason.getMsg());
        this.reason = reason;
        this.threshold = threshold;
        this.value = value;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
