package com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.impl;

import com.gtc.opportunity.trader.domain.ClientConfig;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Created by Valentyn Berezin on 31.08.18.
 */
@UtilityClass
final class Util {

    BigDecimal avg(BigDecimal... values) {
        BigDecimal res = BigDecimal.ZERO;
        for (BigDecimal val : values) {
            res = res.add(val);
        }

        return res.divide(new BigDecimal(values.length), MathContext.DECIMAL128);
    }

    BigDecimal ceilAmount(ClientConfig config, double amount) {
        return BigDecimal.valueOf(amount).setScale(config.getScaleAmount(), RoundingMode.CEILING);
    }

    BigDecimal floorAmount(ClientConfig config, double amount) {
        return BigDecimal.valueOf(amount).setScale(config.getScaleAmount(), RoundingMode.FLOOR);
    }

    double computeGain(ClientConfig cfg) {
        return BigDecimal.ONE.add(cfg.getNnConfig().getFuturePriceGainPct().movePointLeft(2)).doubleValue();
    }

    BigDecimal ceilPrice(ClientConfig config, double price) {
        return BigDecimal.valueOf(price).setScale(config.getScalePrice(), RoundingMode.CEILING);
    }

    BigDecimal floorPrice(ClientConfig config, double price) {
        return BigDecimal.valueOf(price).setScale(config.getScalePrice(), RoundingMode.FLOOR);
    }

    BigDecimal calculateAmount(ClientConfig config, BigDecimal minPrice) {
        return calculateMinAmountWithNotional(config, minPrice)
                .setScale(config.getScaleAmount(), RoundingMode.CEILING);
    }

    double computeCharge(ClientConfig config) {
        return BigDecimal.ONE.subtract(config.getTradeChargeRatePct().movePointLeft(2)).doubleValue();
    }

    private BigDecimal calculateMinAmountWithNotional(ClientConfig cfg, BigDecimal price) {
        if (null == cfg.getMinOrderInToCurrency()) {
            return cfg.getMinOrder();
        }

        return cfg.getMinOrder().max(cfg.getMinOrderInToCurrency().divide(price, MathContext.DECIMAL128));
    }
}
