package com.gtc.opportunity.trader.domain;

import com.google.common.base.Ascii;
import lombok.experimental.UtilityClass;

/**
 * Created by Valentyn Berezin on 27.02.18.
 */
@UtilityClass
public class LongMessageLimiter {

    private static final int MAX_LENGTH = 512;

    public String trunc(String from, int length) {
        return Ascii.truncate(from, MAX_LENGTH, "...");
    }

    public String trunc(String from) {
        return trunc(from, MAX_LENGTH);
    }
}
