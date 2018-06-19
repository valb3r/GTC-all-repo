package com.gtc.opportunity.trader.service.stat;

import com.gtc.opportunity.trader.cqe.domain.CrossMarketOpportunity;
import com.gtc.opportunity.trader.domain.stat.MainKey;
import com.gtc.opportunity.trader.domain.stat.StatId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 01.04.18.
 */
@Service
public final class KeyExtractor {

    @Value("${app.stats.profitHistogramResolutionPct}")
    private double profitHistogramResolutionPct;

    public MainKey extractKeyOmitDate(CrossMarketOpportunity opportunity) {
        return new MainKey(opportunity.getClientFrom(), opportunity.getClientTo(),
                opportunity.getCurrencyFrom(), opportunity.getCurrencyTo(),
                computeProfitGroup(opportunity));
    }

    private StatId.ProfitGroup computeProfitGroup(CrossMarketOpportunity op) {
        double currWin = op.getHistWin().getCurr() * 100.0 - 100.0;
        double divs = currWin / profitHistogramResolutionPct;
        long minPos = (long) Math.floor(divs);
        return new StatId.ProfitGroup(
                BigDecimal.valueOf(minPos).multiply(BigDecimal.valueOf(profitHistogramResolutionPct)),
                BigDecimal.valueOf(minPos + 1).multiply(BigDecimal.valueOf(profitHistogramResolutionPct))
        );
    }
}
