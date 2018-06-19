package com.gtc.tradinggateway.aspect.rate;

/**
 * Created by Valentyn Berezin on 27.02.18.
 */
public class RateTooHighException extends IllegalStateException {

    public RateTooHighException() {
    }

    public RateTooHighException(String s) {
        super(s);
    }

    public RateTooHighException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public RateTooHighException(Throwable throwable) {
        super(throwable);
    }
}
