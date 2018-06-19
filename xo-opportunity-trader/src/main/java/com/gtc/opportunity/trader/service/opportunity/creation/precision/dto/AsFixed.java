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

    public AsFixed(BigDecimal value) {
        scale = (short) value.scale();
        val = value.movePointRight(scale).longValueExact();
        approx = value.doubleValue();
        scaleVal = (long) Math.pow(10, scale);
    }

    private AsFixed(double approx, long val, short scale, long scaleVal) {
        this.approx = approx;
        this.val = val;
        this.scale = scale;
        this.scaleVal = scaleVal;
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
        double div = Math.ceil(amount * scaleVal);
        return new AsFixed(div / scaleVal, (long) div, scale, scaleVal);
    }

    public AsFixed floor(double amount) {
        double div = Math.floor(amount * scaleVal);
        return new AsFixed(div / scaleVal, (long) div, scale, scaleVal);
    }

    public AsFixed addToVal(long delta) {
        return new AsFixed(approx - (double) delta / scaleVal, val - delta, scale, scaleVal);
    }
}
