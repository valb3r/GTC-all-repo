package com.gtc.opportunity.trader.service.opportunity.replenishment.precision;

import com.google.common.collect.ImmutableList;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.service.dto.PreciseReplenishAmountDto;
import com.gtc.opportunity.trader.service.dto.SatisfyReplenishAmountDto;
import com.gtc.opportunity.trader.service.opportunity.common.FitterService;
import com.gtc.opportunity.trader.service.opportunity.common.dto.FittedReplenish;
import com.gtc.opportunity.trader.service.opportunity.replenishment.precision.optaplan.XoBalanceScore;
import com.gtc.opportunity.trader.service.opportunity.replenishment.precision.optaplan.XoReplenishPrice;
import com.gtc.opportunity.trader.service.opportunity.replenishment.precision.optaplan.XoTrade;
import com.gtc.opportunity.trader.service.opportunity.replenishment.precision.optaplan.XoTradeBalance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.domain.valuerange.CountableValueRange;
import org.optaplanner.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicType;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
import org.optaplanner.core.config.localsearch.LocalSearchType;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaplanner.core.impl.domain.valuerange.buildin.bigdecimal.BigDecimalValueRange;
import org.optaplanner.core.impl.domain.valuerange.buildin.collection.ListValueRange;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 04.04.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreciseReplenishmentCalculator {

    private final FitterService fit;

    @Transactional
    public PreciseReplenishAmountDto searchPrecisely(SatisfyReplenishAmountDto sat) {
        FittedReplenish fittedXo = fit.fit(sat);
        log.info("Solving for {} replenish as {}", sat, fittedXo);

        Solver<XoTradeBalance> solver = buildSolver(Math.min(
                sat.getFrom().getMaxSolveReplenishTimeMs(),
                sat.getTo().getMaxSolveReplenishTimeMs())
        );

        XoTradeBalance problem = new XoTradeBalance(
                buildTradePrice(fittedXo, sat.getFrom(), sat.getTo()),
                new XoTrade(fittedXo.getMinBuyAmount(), fittedXo.getMinSellAmount(),
                        fittedXo.getTargetBuyPrice(), fittedXo.getTargetSellPrice()),
                null,
                buyRange(fittedXo),
                sellRange(fittedXo),
                priceBuyRange(fittedXo),
                priceSellRange(fittedXo)
        );
        problem.setScore((HardSoftBigDecimalScore) new XoBalanceScore().calculateScore(problem));

        XoTradeBalance solved = solver.solve(problem);

        if (solved.getScore().getHardScore().compareTo(BigDecimal.ZERO) != 0) {
            log.info("Hard constrained not met {}", solved);
            throw new IllegalStateException("Hard constrained not met");
        }

        return new PreciseReplenishAmountDto(
                solved.getTrade().getSellToPrice(),
                solved.getTrade().getSellAmountTo(),
                solved.getTrade().getBuyFromPrice(),
                solved.getTrade().getBuyAmountFrom(),
                solved.getTrade().getBuyAmountFrom().multiply(solved.getPrice().getLossFrom())
                        .subtract(solved.getTrade().getSellAmountTo()),
                sat.getFrom(),
                sat.getTo()
        );
    }

    private Solver<XoTradeBalance> buildSolver(int solveForMs) {
        SolverFactory<XoTradeBalance> solverFactory = SolverFactory.createEmpty();
        SolverConfig cfg = solverFactory.getSolverConfig();
        cfg.setSolutionClass(XoTradeBalance.class);
        cfg.setEntityClassList(ImmutableList.of(XoTrade.class));
        cfg.setTerminationConfig(new TerminationConfig());
        cfg.getTerminationConfig().setMillisecondsSpentLimit((long) solveForMs);
        cfg.setScoreDirectorFactoryConfig(new ScoreDirectorFactoryConfig());
        cfg.getScoreDirectorFactoryConfig().setEasyScoreCalculatorClass(XoBalanceScore.class);
        ConstructionHeuristicPhaseConfig constr = new ConstructionHeuristicPhaseConfig();
        constr.setConstructionHeuristicType(ConstructionHeuristicType.ALLOCATE_FROM_POOL);
        LocalSearchPhaseConfig localCfg = new LocalSearchPhaseConfig();
        localCfg.setLocalSearchType(LocalSearchType.LATE_ACCEPTANCE);
        cfg.setPhaseConfigList(ImmutableList.of(constr, localCfg));

        return solverFactory.buildSolver();
    }

    private XoReplenishPrice buildTradePrice(FittedReplenish fittedXo,
                                             ClientConfig from, ClientConfig to) {
        return new XoReplenishPrice(
                getLoss(from.getTradeChargeRatePct()),
                getLoss(to.getTradeChargeRatePct()),
                fittedXo.getTargetBuyPrice(),
                fittedXo.getTargetSellPrice()
        );
    }

    private CountableValueRange<BigDecimal> sellRange(FittedReplenish fittedXo) {

        if (0 == fittedXo.getMinSellAmount().compareTo(fittedXo.getMaxSellAmount())) {
            return new ListValueRange<>(ImmutableList.of(fittedXo.getMinSellAmount()));
        }

        return new BigDecimalValueRange(
                fittedXo.getMinSellAmount(),
                fittedXo.getMaxSellAmount(),
                fittedXo.getAmountGridStepSell()
        );
    }

    private CountableValueRange<BigDecimal> buyRange(FittedReplenish fittedXo) {

        if (0 == fittedXo.getMinBuyAmount().compareTo(fittedXo.getMaxBuyAmount())) {
            return new ListValueRange<>(ImmutableList.of(fittedXo.getMinBuyAmount()));
        }

        return new BigDecimalValueRange(
                fittedXo.getMinBuyAmount(),
                fittedXo.getMaxBuyAmount(),
                fittedXo.getAmountGridStepBuy()
        );
    }

    private CountableValueRange<BigDecimal> priceSellRange(FittedReplenish fittedXo) {

        return new BigDecimalValueRange(
                BigDecimal.ZERO
                        .setScale(fittedXo.getPriceGridStepSell().scale(), BigDecimal.ROUND_CEILING),
                fittedXo.getTargetSellPrice().multiply(BigDecimal.TEN)
                        .setScale(fittedXo.getPriceGridStepSell().scale(), BigDecimal.ROUND_CEILING),
                fittedXo.getPriceGridStepSell()
        );
    }

    private CountableValueRange<BigDecimal> priceBuyRange(FittedReplenish fittedXo) {

        return new BigDecimalValueRange(
                BigDecimal.ZERO
                        .setScale(fittedXo.getPriceGridStepBuy().scale(), BigDecimal.ROUND_CEILING),
                fittedXo.getTargetBuyPrice().multiply(BigDecimal.TEN)
                        .setScale(fittedXo.getPriceGridStepSell().scale(), BigDecimal.ROUND_CEILING),
                fittedXo.getPriceGridStepBuy()
        );
    }

    private BigDecimal getLoss(BigDecimal lossPct) {
        return BigDecimal.ONE.subtract(lossPct.movePointLeft(2));
    }
}
