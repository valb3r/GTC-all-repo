package com.gtc.opportunity.trader.service.opportunity.creation.precision.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by Valentyn Berezin on 10.04.18.
 */
@Data
public class AsFixed {

    private final double approx;
    private final long val;
    private final short scale;
    private final long scaleVal;
    private final boolean negativeScale;

    public AsFixed(BigDecimal value) {
        scale = (short) value.scale();
        val = value.movePointRight(scale).longValueExact();
        approx = value.doubleValue();
        scaleVal = (long) Math.pow(10, Math.abs(scale));
        negativeScale = scale < 0;
    }

    private AsFixed(double approx, long val, short scale, long scaleVal, boolean negativeScale) {
        this.approx = approx;
        this.val = val;
        this.scale = scale;
        this.scaleVal = scaleVal;
        this.negativeScale = negativeScale;
    }

    public BigDecimal scale(long value) {
        return BigDecimal.valueOf(value).movePointLeft(scale);
    }

    public BigDecimal value() {
        return scale(val);
    }

    public BigDecimal scaleStep() {
        return BigDecimal.ONE.movePointLeft(scale);
    }

    public AsFixed valueOf(double amount, RoundingMode mode) {
        return new AsFixed(BigDecimal.valueOf(amount).setScale(scale, mode));
    }

    public AsFixed ceil(double amount) {
        if (negativeScale) {
            double div = Math.ceil(amount / scaleVal);
            return new AsFixed(div * scaleVal, (long) div, scale, scaleVal, true);
        }

        double div = Math.ceil(amount * scaleVal);
        return new AsFixed(div / scaleVal, (long) div, scale, scaleVal, false);
    }

    public AsFixed floor(double amount) {
        if (negativeScale) {
            double div = Math.floor(amount / scaleVal);
            return new AsFixed(div * scaleVal, (long) div, scale, scaleVal, true);
        }

        double div = Math.floor(amount * scaleVal);
        return new AsFixed(div / scaleVal, (long) div, scale, scaleVal, false);
    }

    public static boolean scaleGreater(AsFixed one, AsFixed two) {
        if (one.negativeScale) {
            if (two.negativeScale) {
                return one.scale < two.scale;
            }
            return true;
        }

        if (two.negativeScale) {
            return false;
        }

        return one.scale > two.scale;
    }
}
