package com.gtc.opportunity.trader.service.opportunity.creation.precision;

import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.AsFixed;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.XoClientTradeConditionAsLong;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.XoTradeCondition;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by Valentyn Berezin on 07.04.18.
 */
@Service
public class ToLongMathMapper {

    public XoClientTradeConditionAsLong map(XoTradeCondition condition) {
        return XoClientTradeConditionAsLong.builder()
                // invert
                .minToBuyAmount(toAmount(condition.getMinToSellAmount(), condition))
                .maxToBuyAmount(toAmount(condition.getMaxToSellAmount(), condition))
                .minFromSellAmount(fromAmount(condition.getMinFromBuyAmount(), condition))
                .maxFromSellAmount(fromAmount(condition.getMaxFromBuyAmount(), condition))
                .minToBuyPrice(toPrice(condition.getMinToSellPrice(), condition))
                .maxFromSellPrice(fromPrice(condition.getMaxFromBuyPrice(), condition))
                .amountSafetyFromCoef(new AsFixed(condition.getAmountSafetyFromCoef().stripTrailingZeros()))
                .amountSafetyToCoef(new AsFixed(condition.getAmountSafetyToCoef().stripTrailingZeros()))
                .lossFromCoef(new AsFixed(condition.getLossFromCoef().stripTrailingZeros()))
                .lossToCoef(new AsFixed(condition.getLossToCoef().stripTrailingZeros()))
                .minProfitCoef(new AsFixed(condition.getRequiredProfitCoef().stripTrailingZeros()))
                // no invert
                .marketSellTo(condition.getSellTo())
                .marketBuyFrom(condition.getBuyFrom())
                .build();
    }

    private static AsFixed fromAmount(double amount, XoTradeCondition condition) {
        return new AsFixed(stepped(amount, condition.getStepFromAmountPow10(), RoundingMode.FLOOR));
    }

    private static AsFixed toAmount(double amount, XoTradeCondition condition) {
        return new AsFixed(stepped(amount, condition.getStepToAmountPow10(), RoundingMode.FLOOR));
    }

    private static AsFixed fromPrice(double price, XoTradeCondition condition) {
        return new AsFixed(stepped(price, condition.getStepFromPricePow10(), RoundingMode.FLOOR));
    }

    private static AsFixed toPrice(double price, XoTradeCondition condition) {
        return new AsFixed(stepped(price, condition.getStepToPricePow10(), RoundingMode.CEILING));
    }

    private static BigDecimal stepped(double amount, BigDecimal pow10step, RoundingMode mode) {
        int scale = pow10step.stripTrailingZeros().scale();
        return BigDecimal.valueOf(amount).setScale(scale, mode);
    }

    static BigDecimal fromAmount(long amount, XoTradeCondition condition) {
        return BigDecimal.valueOf(amount).movePointLeft(condition.getStepFromAmountPow10().scale());
    }

    static BigDecimal toAmount(long amount, XoTradeCondition condition) {
        return BigDecimal.valueOf(amount).movePointLeft(condition.getStepToAmountPow10().scale());
    }

    static BigDecimal fromPrice(long price, XoTradeCondition condition) {
        return BigDecimal.valueOf(price).movePointLeft(condition.getStepFromPricePow10().scale());
    }

    static BigDecimal toPrice(long price, XoTradeCondition condition) {
        return BigDecimal.valueOf(price).movePointLeft(condition.getStepToPricePow10().scale());
    }
}
