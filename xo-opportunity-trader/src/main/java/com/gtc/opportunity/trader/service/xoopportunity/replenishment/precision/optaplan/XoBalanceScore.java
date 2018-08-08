package com.gtc.opportunity.trader.service.xoopportunity.replenishment.precision.optaplan;

import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;

import java.math.BigDecimal;

/**
 *  | buyFrom * lossFrom - sellTo >= 0 (we loose nothing)
 * <
 *  | - buyFrom * buyFromPrice + sellTo * sellToPrice * lossTo >= 0 (we loose nothing)
 *  |  soft = buyFrom - sellTo
 */
@Slf4j
public class XoBalanceScore implements EasyScoreCalculator<XoTradeBalance> {

    @Override
    public Score calculateScore(XoTradeBalance bal) {

        BigDecimal acceptLoss = acceptLoss(bal);
        BigDecimal noReverseLoss = noReverseLoss(bal);
        BigDecimal bestEffort = bestEffort(bal);

        return HardSoftBigDecimalScore.valueOf(acceptLoss.add(noReverseLoss), bestEffort);
    }

    private BigDecimal acceptLoss(XoTradeBalance bal) {
        BigDecimal loss = bal.getTrade().getBuyAmountFrom().multiply(bal.getPrice().getLossFrom())
                .subtract(bal.getTrade().getSellAmountTo());
        return trimPositive(loss);
    }

    private BigDecimal noReverseLoss(XoTradeBalance bal) {
        BigDecimal reverse = bal.getTrade().getSellAmountTo()
                .multiply(bal.getTrade().getSellToPrice()).multiply(bal.getPrice().getLossTo())
                .subtract(bal.getTrade().getBuyAmountFrom().multiply(bal.getTrade().getBuyFromPrice()));

        return trimPositive(reverse);
    }

    private BigDecimal bestEffort(XoTradeBalance bal) {
        BigDecimal fromDelta = bal.getTrade().getBuyFromPrice().subtract(bal.getPrice().getTargetBuyPrice());
        BigDecimal toDelta = bal.getPrice().getTargetSellPrice().subtract(bal.getTrade().getSellToPrice());
        return trimPositive(fromDelta).multiply(trimPositive(fromDelta))
                .add(trimPositive(toDelta).multiply(trimPositive(toDelta))).negate();
    }

    private static BigDecimal trimPositive(BigDecimal val) {
        if (val.compareTo(BigDecimal.ZERO) > 0) {
            return BigDecimal.ZERO;
        }

        return val;
    }
}
