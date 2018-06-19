package com.gtc.opportunity.trader.service.opportunity.creation.precision;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by Valentyn Berezin on 10.04.18.
 */
@UtilityClass
final class RoundingUtil {

    long longVal(BigDecimal value, int scale, RoundingMode mode) {
        return value.movePointRight(scale)
                .setScale(0, mode)
                .longValueExact();
    }
}
