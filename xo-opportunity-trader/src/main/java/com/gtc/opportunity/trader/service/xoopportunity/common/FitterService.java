package com.gtc.opportunity.trader.service.xoopportunity.common;

import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.service.dto.SatisfyReplenishAmountDto;
import com.gtc.opportunity.trader.service.xoopportunity.common.dto.FittedReplenish;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Created by Valentyn Berezin on 24.03.18.
 */
@Service
@RequiredArgsConstructor
public class FitterService {

    public FittedReplenish fit(SatisfyReplenishAmountDto amount) {

        return FittedReplenish.builder()
                .targetSellPrice(amount.getSellPrice())
                .targetBuyPrice(amount.getBuyPrice())
                // We can accept some losses due to optimization
                .minSellAmount(amount(amount.getTo().getMinOrder().doubleValue(), amount.getTo(), RoundingMode.CEILING))
                .maxSellAmount(amount(amount.getMaxSellAmount(), amount.getTo(), RoundingMode.FLOOR))
                .minBuyAmount(amount(amount.getFrom().getMinOrder().doubleValue(), amount.getFrom(), RoundingMode.CEILING))
                .maxBuyAmount(amount(amount.getMaxBuyAmount(), amount.getFrom(), RoundingMode.FLOOR))
                .amountGridStepSell(amountGridStep(amount.getTo()))
                .amountGridStepBuy(amountGridStep(amount.getFrom()))
                .priceGridStepBuy(priceGridStep(amount.getFrom()))
                .priceGridStepSell(priceGridStep(amount.getTo()))
                .build();
    }

    public BigDecimal price(double price, ClientConfig config, RoundingMode mode) {
        return doRoundPrice(price, config, mode);
    }

    public BigDecimal amount(double amount, ClientConfig cfg, RoundingMode mode) {
        return clipOrRound(amount, cfg.getMinOrder(), cfg.getMaxOrder(), amountGridStep(cfg), mode);
    }

    private static BigDecimal doRoundPrice(double price, ClientConfig cfg, RoundingMode mode) {
        return round(price, BigDecimal.ONE.movePointLeft(cfg.getScalePrice()), mode);
    }

    private static BigDecimal amountGridStep(ClientConfig cfg) {
        return BigDecimal.ONE.movePointLeft(cfg.getScaleAmount());
    }

    private static BigDecimal priceGridStep(ClientConfig cfg) {
        return BigDecimal.ONE.movePointLeft(cfg.getScalePrice());
    }

    private static BigDecimal round(double value, BigDecimal precision, RoundingMode mode) {
        BigDecimal trgt = BigDecimal.valueOf(value);
        return trgt.divide(precision, MathContext.DECIMAL128).setScale(0, mode).multiply(precision);
    }

    private static BigDecimal clipOrRound(double value, BigDecimal min, BigDecimal max, BigDecimal precision,
                                  RoundingMode mode) {
        BigDecimal trgt = BigDecimal.valueOf(value);
        if (trgt.compareTo(min) < 0) {
            return min.setScale(precision.scale(), mode);
        }

        if (trgt.compareTo(max) > 0) {
            return max.setScale(precision.scale(), mode);
        }

        return round(value, precision, mode);
    }
}
