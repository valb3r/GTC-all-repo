package com.gtc.opportunity.trader.service.opportunity.creation;

import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.RejectionException;
import lombok.experimental.UtilityClass;

/**
 * Created by Valentyn Berezin on 01.04.18.
 */
@UtilityClass
public final class Checker {

    public void validateAtLeast(Reason reason, double value, double threshold) {
        if (value >= threshold) {
            return;
        }

        throw new RejectionException(reason, value, threshold);
    }
}
