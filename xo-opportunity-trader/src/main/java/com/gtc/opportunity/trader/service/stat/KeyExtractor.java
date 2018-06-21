package com.gtc.opportunity.trader.service.stat;

import com.gtc.opportunity.trader.cqe.domain.CrossMarketOpportunity;
import com.gtc.opportunity.trader.domain.stat.MainKey;
import com.gtc.opportunity.trader.domain.stat.StatId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Created by Valentyn Berezin on 01.04.18.
 */
@Service
public final class KeyExtractor {

    @Value("${app.stats.profitHistogramResolutionPct}")
    private double profitHistogramResolutionPct;

    public MainKey extractKeyOmitDate(CrossMarketOpportunity opportunity) {
        return extractKeyOmitDate(opportunity, op -> op.getHistWin().getCurr());
    }

    public MainKey extractKeyOmitDate(
            CrossMarketOpportunity opportunity, Function<CrossMarketOpportunity, Double> winExtractor) {
        return new MainKey(opportunity.getClientFrom(), opportunity.getClientTo(),
                opportunity.getCurrencyFrom(), opportunity.getCurrencyTo(),
                computeProfitGroup(opportunity, winExtractor));
    }

    private StatId.ProfitGroup computeProfitGroup(
            CrossMarketOpportunity op, Function<CrossMarketOpportunity, Double> winExtractor) {
        double currWin = winExtractor.apply(op) * 100.0 - 100.0;
        double divs = currWin / profitHistogramResolutionPct;
        long minPos = (long) Math.floor(divs);
        return new StatId.ProfitGroup(
                BigDecimal.valueOf(minPos).multiply(BigDecimal.valueOf(profitHistogramResolutionPct)),
                BigDecimal.valueOf(minPos + 1).multiply(BigDecimal.valueOf(profitHistogramResolutionPct))
        );
    }
}
