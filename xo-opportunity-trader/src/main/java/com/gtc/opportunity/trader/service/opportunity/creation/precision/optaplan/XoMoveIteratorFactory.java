package com.gtc.opportunity.trader.service.opportunity.creation.precision.optaplan;

import com.google.common.collect.ImmutableList;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.AsFixed;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;
import org.optaplanner.core.impl.score.director.ScoreDirector;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Generates moves along 'ideal' double solution.
 *  | soft:
 *  | -sellFromAmount + buyToAmount * lossTo >= profit * sellFromAmount (profit = profitPct/100) = profit eq = max.
 * <
 *  | hard:
 *  | sellFromAmount * sellFromPrice * lossFrom - buyToAmount * buyToPrice >= 0 = no loss of paired currency
 *
 *  1. Random sellFromAmount -> round it
 *  2. Approx. buyToAmount from soft +/- round with noise
 *  3. Random sellFromPrice -> round it
 *  4. Approx. buyToPrice from hard +/- round with noise
 */
@Slf4j
public class XoMoveIteratorFactory implements MoveIteratorFactory<XoTradeBalance> {

    @Override
    public long getSize(ScoreDirector<XoTradeBalance> scoreDirector) {
        return Integer.MAX_VALUE - 1L;
    }

    @Override
    public Iterator<? extends Move<XoTradeBalance>> createOriginalMoveIterator(ScoreDirector<XoTradeBalance> scoreDirector) {
        throw new IllegalStateException("Original move iterator not supported");
    }

    @Override
    public Iterator<? extends Move<XoTradeBalance>> createRandomMoveIterator(
            ScoreDirector<XoTradeBalance> scoreDirector,
            Random workingRandom) {
        return new RandomIterator(scoreDirector.getWorkingSolution(), workingRandom);
    }

    private static class RandomIterator implements Iterator<Move<XoTradeBalance>> {

        private static final int STEP_RANGE = 100;
        private static final double DOUBLE_EPS = 1e-12;

        private static final int DISTORT_RANGE = 10;
        private static final int N_DISTORT = 10;

        private final XoTradeBalance solution;
        private final Iterator<Double> sellFromAmountRandom;
        private final Iterator<Double> sellFromPriceRandom;
        private final Iterator<Double> buyToAmountRandom;
        private final Iterator<Double> buyToPriceRandom;
        private final Iterator<Integer> dice;
        private final Iterator<Integer> distort;
        private final double profitCoef;
        private final double lossFrom;
        private final double lossTo;

        private final Queue<XoMove> moves = new LinkedBlockingQueue<>();

        RandomIterator(XoTradeBalance solution, Random workingRandom) {
            this.solution = solution;
            sellFromAmountRandom = workingRandom.doubles(
                    solution.getConstraint().getMinFromSellAmount().getApprox() - DOUBLE_EPS,
                    solution.getConstraint().getMaxFromSellAmount().getApprox()
            ).iterator();

            sellFromPriceRandom = workingRandom.doubles(
                    Arrays.stream(solution.getConstraint().getMarketBuyFrom())
                            .mapToDouble(FullCrossMarketOpportunity.Histogram::getMinPrice)
                            .min().orElse(0.0) - DOUBLE_EPS,
                    solution.getConstraint().getMaxFromSellPrice().getApprox()
            ).iterator();

            buyToAmountRandom = workingRandom.doubles(
                    solution.getConstraint().getMinToBuyAmount().getApprox()- DOUBLE_EPS,
                    solution.getConstraint().getMaxToBuyAmount().getApprox()
            ).iterator();

            buyToPriceRandom = workingRandom.doubles(
                    solution.getConstraint().getMinToBuyPrice().getApprox() - DOUBLE_EPS,
                    Arrays.stream(solution.getConstraint().getMarketSellTo())
                            .mapToDouble(FullCrossMarketOpportunity.Histogram::getMaxPrice)
                            .max().orElse(0.0)
            ).iterator();

            dice = workingRandom.ints(-STEP_RANGE, STEP_RANGE).iterator();
            distort = workingRandom.ints(-DISTORT_RANGE, DISTORT_RANGE).iterator();

            profitCoef = solution.getConstraint().getMinProfitCoef().getApprox();
            lossFrom = solution.getConstraint().getLossFromCoef().getApprox();
            lossTo = solution.getConstraint().getLossToCoef().getApprox();
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Move<XoTradeBalance> next() {
            if (moves.isEmpty()) {
                moves.addAll(buildPack());
            }

            return moves.poll();
        }

        private List<XoMove> buildPack() {
            XoMove ini = AsFixed.scaleGreater(
                    solution.getConstraint().getMinFromSellAmount(),
                    solution.getConstraint().getMinToBuyAmount()
            ) ?  buildMoveSellDriven() : buildMoveBuyDriven();

            Set<XoMove> pack = new HashSet<>();
            pack.add(ini);

            for (int i = 0; i < N_DISTORT; i++) {
                XoMove distorted = new XoMove(
                        ini.getEntity(),
                        ini.getSellAmountFrom() + distort.next(),
                        ini.getBuyAmountTo() + distort.next(),
                        ini.getSellPriceFrom() + distort.next(),
                        ini.getBuyPriceTo() + distort.next()
                );

                if (distorted.isDoable()) {
                    pack.add(distorted);
                }
            }

            return new ArrayList<>(pack);
        }

        private XoMove buildMoveSellDriven() {
            AsFixed sellFromAmount = randomRound(
                    sellFromAmountRandom.next(),
                    solution.getConstraint().getMinFromSellAmount()
            );

            AsFixed buyToAmount = solution.getConstraint().getMinToBuyAmount().ceil(
                    sellFromAmount.getApprox() * profitCoef / lossTo
            );

            AsFixed sellFromPrice = randomRound(
                    sellFromPriceRandom.next(),
                    solution.getConstraint().getMaxFromSellPrice()
            );

            AsFixed buyToPrice = solution.getConstraint().getMinToBuyPrice().floor(
                    sellFromAmount.getApprox() * sellFromPrice.getApprox() * lossFrom / buyToAmount.getApprox()
            );

            return new XoMove(
                    solution.getTrade(),
                    sellFromAmount.getVal(),
                    buyToAmount.getVal(),
                    sellFromPrice.getVal(),
                    buyToPrice.getVal()
            );
        }

        private XoMove buildMoveBuyDriven() {
            AsFixed buyToAmount = randomRound(
                    buyToAmountRandom.next(),
                    solution.getConstraint().getMinToBuyAmount()
            );

            AsFixed sellFromAmount = solution.getConstraint().getMinFromSellAmount().floor(
                    buyToAmount.getApprox() / profitCoef * lossTo
            );

            AsFixed buyToPrice = randomRound(
                    buyToPriceRandom.next(),
                    solution.getConstraint().getMinToBuyPrice()
            );

            AsFixed sellFromPrice = solution.getConstraint().getMaxFromSellPrice().ceil(
                    buyToPrice.getApprox() / (sellFromAmount.getApprox() * lossFrom / buyToAmount.getApprox())
            );

            return new XoMove(
                    solution.getTrade(),
                    sellFromAmount.getVal(),
                    buyToAmount.getVal(),
                    sellFromPrice.getVal(),
                    buyToPrice.getVal()
            );
        }

        AsFixed randomRound(double value, AsFixed orig) {
            AsFixed jumped = withJump(value, orig);

            if (0 == jumped.getVal()) {
                return orig.ceil(value);
            }

            return jumped;
        }

        AsFixed withJump(double value, AsFixed orig) {
            if (dice.next() <= 0) {
                return orig.ceil(value);
            }

            AsFixed pre = orig.floor(value);
            if (0 == pre.getVal()) {
                return orig.ceil(value);
            }

            return pre;
        }
    }

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class XoMove extends AbstractMove<XoTradeBalance> {

        private final XoTrade entity;
        private final long sellAmountFrom;
        private final long buyAmountTo;
        private final long sellPriceFrom;
        private final long buyPriceTo;

        @Override
        protected AbstractMove<XoTradeBalance> createUndoMove(ScoreDirector<XoTradeBalance> scoreDirector) {
            return new XoMove(entity, entity.getSellAmountFrom(), entity.getBuyAmountTo(),
                    entity.getSellPriceFrom(), entity.getBuyPriceTo());
        }

        @Override
        protected void doMoveOnGenuineVariables(ScoreDirector<XoTradeBalance> scoreDirector) {
            scoreDirector.beforeVariableChanged(entity, "sellAmountFrom");
            entity.setSellAmountFrom(sellAmountFrom);
            scoreDirector.afterVariableChanged(entity, "sellAmountFrom");

            scoreDirector.beforeVariableChanged(entity, "buyAmountTo");
            entity.setBuyAmountTo(buyAmountTo);
            scoreDirector.afterVariableChanged(entity, "buyAmountTo");

            scoreDirector.beforeVariableChanged(entity, "sellPriceFrom");
            entity.setSellPriceFrom(sellPriceFrom);
            scoreDirector.afterVariableChanged(entity, "sellPriceFrom");

            scoreDirector.beforeVariableChanged(entity, "buyPriceTo");
            entity.setBuyPriceTo(buyPriceTo);
            scoreDirector.afterVariableChanged(entity, "buyPriceTo");
        }

        boolean isDoable() {
            return sellAmountFrom > 0 && buyAmountTo > 0 && sellPriceFrom > 0 && buyPriceTo > 0;
        }

        @Override
        public boolean isMoveDoable(ScoreDirector<XoTradeBalance> scoreDirector) {
            return isDoable();
        }

        @Override
        public Collection<?> getPlanningEntities() {
            return Collections.singleton(entity);
        }

        @Override
        public Collection<?> getPlanningValues() {
            return ImmutableList.of(sellAmountFrom, buyAmountTo, sellPriceFrom, buyPriceTo);
        }
    }
}
