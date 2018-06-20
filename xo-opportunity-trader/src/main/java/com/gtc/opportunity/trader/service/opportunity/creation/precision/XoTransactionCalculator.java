package com.gtc.opportunity.trader.service.opportunity.creation.precision;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.RateLimiter;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.service.dto.PreciseXoAmountDto;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.RejectionException;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.AsFixed;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.XoClientTradeConditionAsLong;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.XoTradeCondition;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.optaplan.XoBalanceScore;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.optaplan.XoTrade;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.optaplan.XoTradeBalance;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.domain.valuerange.CountableValueRange;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.impl.domain.valuerange.buildin.collection.ListValueRange;
import org.optaplanner.core.impl.domain.valuerange.buildin.primlong.LongValueRange;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Valentyn Berezin on 07.04.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XoTransactionCalculator {

    private static final String SOLVER_CONFIG_LOCATION = "optaplan/config/xo/solver.xml";

    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    private final ToLongMathMapper mapper;
    private final HistogramIntegrator integrator;

    /**
     * Inverting market condition in {@param condition} into client trades terms.
     * Safety coef. are used to satisfy using given price, amount:
     * amountHistogram(best price * (1 + safetyPrice), price) >= amount * (1 + safetyAmount)
     * where i.e. best price * (1 + safetyPrice) = {@link XoTradeCondition#minToSellPrice} for buy on `to`
     * @param condition trade condition range in market terms.
     * @return distributed transaction that will satisfy both.
     */
    public PreciseXoAmountDto calculate(XoTradeCondition condition) {
        if (!limiters.computeIfAbsent(condition.getKey(), id -> RateLimiter.create(condition.getPermits()))
                .tryAcquire()) {
            throw new RejectionException(Reason.TOO_FREQUENT_SOLVE);
        }

        XoTradeBalance bal = calculate(SOLVER_CONFIG_LOCATION, condition);

        if (!bal.getScore().isFeasible()) {
            log.info("Hard constraint not met {}", bal);
            throw new RejectionException(Reason.OPT_CONSTR_FAIL);
        }

        // read maximum soft score (max profit)
        PreciseXoAmountDto result = mapToResult(condition, bal);
        validateResult(condition, result);
        return result;
    }

    private XoTradeBalance calculate(String confPath, XoTradeCondition condition) {
        log.info("Solving for {}", condition);
        Solver<XoTradeBalance> solver = buildSolver(confPath, condition.getMaxSolveTimeMs());

        XoClientTradeConditionAsLong fitted = mapper.map(condition);
        XoTradeBalance problem = new XoTradeBalance(
                fitted,
                initial(fitted),
                null,
                range(fitted.getMinFromSellAmount().getVal(), fitted.getMaxFromSellAmount().getVal()),
                range(fitted.getMinToBuyAmount().getVal(), fitted.getMaxToBuyAmount().getVal()),
                range(minPriceHist(fitted.getMarketBuyFrom(), fitted.getMaxFromSellPrice()),
                        fitted.getMaxFromSellPrice().getVal()),
                range(fitted.getMinToBuyPrice().getVal(),
                        maxPriceHist(fitted.getMarketSellTo(), fitted.getMinToBuyPrice())),
                integrator
        );

        problem.setScore(new XoBalanceScore().calculateScore(problem));
        log.info("Solving for problem {}", problem);

        return solver.solve(problem);
    }

    private PreciseXoAmountDto mapToResult(XoTradeCondition condition, XoTradeBalance solved) {
        BigDecimal sellAmount = ToLongMathMapper.fromAmount(solved.getTrade().getSellAmountFrom(), condition);
        BigDecimal buyAmount = ToLongMathMapper.toAmount(solved.getTrade().getBuyAmountTo(), condition);
        BigDecimal sellPrice = ToLongMathMapper.fromPrice(solved.getTrade().getSellPriceFrom(), condition);
        BigDecimal buyPrice = ToLongMathMapper.toPrice(solved.getTrade().getBuyPriceTo(), condition);
        BigDecimal profit = buyAmount.multiply(solved.getConstraint().getLossToCoef().value()).subtract(sellAmount);
        return new PreciseXoAmountDto(
                sellPrice,
                sellAmount,
                buyPrice,
                buyAmount,
                profit,
                profit.doubleValue() / sellAmount.abs().doubleValue() * 100.0
        );
    }

    private void validateResult(XoTradeCondition condition, PreciseXoAmountDto finalValue) {
        BigDecimal noLoss = finalValue.getSellAmount().multiply(finalValue.getSellPrice())
                .multiply(condition.getLossFromCoef())
                .subtract(
                        finalValue.getBuyPrice().multiply(finalValue.getBuyAmount())
                );
        if (noLoss.compareTo(BigDecimal.ZERO) < 0 ||
                finalValue.getProfit().compareTo(
                        finalValue.getSellAmount().multiply(condition.getRequiredProfitCoef().subtract(BigDecimal.ONE))
                ) < 0) {
            log.info("Solution validation failed {} / {}", condition, finalValue);
            throw new RejectionException(Reason.SOL_VALID_FAIL);
        }
    }

    private XoTrade initial(XoClientTradeConditionAsLong condition) {
        double gain = condition.getMaxFromSellPrice().value().doubleValue() /
                condition.getMinToBuyPrice().value().doubleValue();
        BigDecimal minSellAmount = condition.getMinFromSellAmount().value();
        BigDecimal maxSellAmount = condition.getMaxFromSellAmount().value();
        BigDecimal minBuyAmount = condition.getMinToBuyAmount().value();
        BigDecimal maxBuyAmount = condition.getMaxToBuyAmount().value();

        double sell = maxSellAmount.doubleValue();
        double buy = maxBuyAmount.doubleValue();

        double sellAmount;
        double buyAmount;

        if (0 == minSellAmount.compareTo(maxSellAmount)) {
            sellAmount = sell;
            buyAmount = sell * gain;
        } else if (0 == minBuyAmount.compareTo(maxBuyAmount)) {
            sellAmount = buy / gain;
            buyAmount = buy;
        } else {
            double amount = Math.min(sell, buy);
            IniAmounts amounts = initialSellBuyAmount(amount, gain, condition);
            // pre-divide by gain to imitate safety
            buyAmount = amounts.getToBuy();
            sellAmount = amounts.getFromSell();
        }

        return new XoTrade(
                condition.getMinFromSellAmount().valueOf(sellAmount, RoundingMode.FLOOR).getVal(),
                condition.getMinToBuyAmount().valueOf(buyAmount, RoundingMode.CEILING).getVal(),
                condition.getMaxFromSellPrice().getVal(),
                condition.getMinToBuyPrice().getVal()
        );
    }

    private IniAmounts initialSellBuyAmount(double amount, double gain, XoClientTradeConditionAsLong condition) {
        if (condition.getMinToBuyAmount().getScale() > condition.getMinFromSellAmount().getScale()) {
            double sellAmount = BigDecimal.valueOf(amount / gain)
                    .subtract(condition.getMinFromSellAmount().scaleStep())
                    .setScale(condition.getMinFromSellAmount().getScale(), RoundingMode.FLOOR).doubleValue();
            return new IniAmounts(
                    sellAmount,
                    sellAmount * gain - condition.getMinToBuyAmount().scaleStep().doubleValue()
            );
        }

        double buyAmount = BigDecimal.valueOf(amount).subtract(condition.getMinToBuyAmount().scaleStep())
                .setScale(condition.getMinToBuyAmount().getScale(), RoundingMode.FLOOR).doubleValue();
        return new IniAmounts(
                buyAmount / gain - condition.getMinToBuyAmount().scaleStep().doubleValue(),
                buyAmount
        );
    }

    private Solver<XoTradeBalance> buildSolver(String configPath, int solveForMs) {
        SolverFactory<XoTradeBalance> factory = SolverFactory.createFromXmlResource(configPath);
        factory.getSolverConfig().getTerminationConfig().setMillisecondsSpentLimit((long) solveForMs);
        return factory.buildSolver();
    }

    private static CountableValueRange<Long> range(long min, long max) {
        if (min == max) {
            return new ListValueRange<>(ImmutableList.of(min));
        }

        return new LongValueRange(min, max);
    }

    private static long minPriceHist(FullCrossMarketOpportunity.Histogram[] histograms, AsFixed to) {
        double value = Arrays.stream(histograms)
                .mapToDouble(FullCrossMarketOpportunity.Histogram::getMinPrice).min().orElse(0.0);
        return to.valueOf(value, RoundingMode.CEILING).getVal();
    }

    private static long maxPriceHist(FullCrossMarketOpportunity.Histogram[] histograms, AsFixed to) {
        double value = Arrays.stream(histograms)
                .mapToDouble(FullCrossMarketOpportunity.Histogram::getMaxPrice).max().orElse(0.0);
        return to.valueOf(value, RoundingMode.FLOOR).getVal();
    }

    @Data
    private static final class IniAmounts {

        private final double fromSell;
        private final double toBuy;
    }
}
