package com.gtc.opportunity.trader.service.compute;

import com.gtc.opportunity.trader.domain.FeeSystem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Created by Valentyn Berezin on 03.09.18.
 */
@Service
public class TradeBalanceChange {

    public BalanceChange compute(FeeSystem system, BigDecimal feePct, TradeDesc... trades) {
        return compute(Arrays.asList(trades), system, feePct);
    }

    public BalanceChange compute(List<TradeDesc> trades, FeeSystem system, BigDecimal feePct) {
        BalanceChange result = new BalanceChange();
        for (TradeDesc trade : trades) {
            result = result.add(compute(trade, system, feePct));
        }

        return result;
    }

    private BalanceChange compute(TradeDesc trade, FeeSystem system, BigDecimal feePct) {
        Function<BigDecimal, BigDecimal> amount = coef -> trade.getAmount().abs().multiply(coef)
                .setScale(trade.getPrice().scale() + trade.getAmount().scale(), RoundingMode.FLOOR);
        if (trade.isSell()) {
            return new BalanceChange(
                    amount.apply(fee(system, feePct, true, true)).negate(),
                    amount.apply(trade.getPrice().multiply(fee(system, feePct, true, false)))
            );
        }

        return new BalanceChange(
                amount.apply(fee(system, feePct, false, true)),
                amount.apply(trade.getPrice().multiply(fee(system, feePct, false, false))).negate()
        );
    }

    private BigDecimal fee(FeeSystem system, BigDecimal feePct, boolean isSell, boolean isFrom) {
        if (FeeSystem.FEE_BEFORE == system) {
            return isFrom ? BigDecimal.ONE : feeBefore(feePct, isSell);
        }

        if (FeeSystem.FEE_AFTER == system) {
            if (isSell) {
                return isFrom ? BigDecimal.ONE : fee(feePct);
            }

            return isFrom ? fee(feePct) : BigDecimal.ONE;
        }

        throw new IllegalArgumentException("Unknown system " + system.toString());
    }

    private BigDecimal feeBefore(BigDecimal feePct, boolean isSell) {
        return isSell ? fee(feePct) : BigDecimal.ONE.divide(fee(feePct), MathContext.DECIMAL128);
    }

    private BigDecimal fee(BigDecimal feePct) {
        return BigDecimal.ONE.subtract(feePct.movePointLeft(2));
    }
}
