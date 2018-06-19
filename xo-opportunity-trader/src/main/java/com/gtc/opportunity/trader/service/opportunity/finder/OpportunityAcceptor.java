package com.gtc.opportunity.trader.service.opportunity.finder;

import com.google.common.base.Throwables;
import com.gtc.opportunity.trader.cqe.domain.CrossMarketOpportunity;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.service.opportunity.creation.OpportunityAnalyzer;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.RejectionException;
import com.gtc.opportunity.trader.service.stat.RejectedTradeStatService;
import com.gtc.opportunity.trader.service.stat.XoStatService;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunityAcceptor {

    private final OpportunityAnalyzer analyzer;
    private final XoStatService xoStatService;
    private final RejectedTradeStatService rejectedStatService;

    @Trace(dispatcher = true)
    public void ackCreateOrOpenOpportunity(FullCrossMarketOpportunity opportunity) {
        String oldName = Thread.currentThread().getName();
        try {
            setThreadNameByOpportunity(opportunity);
            log.debug("Acknowledged created opportunity {}", opportunity);
            analyzer.newOpportunity(opportunity);
        } catch (RejectionException ex) {
            log.debug("Rejected: {}", ex.getReason());
            rejectedStatService.ackRejection(ex, opportunity);
        } catch (RuntimeException ex) {
            // GCE looses exception traces
            log.error("Exception caught {}", Throwables.getRootCause(ex).toString(), ex);
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }

    @Trace(dispatcher = true)
    public void ackCloseOpportunity(CrossMarketOpportunity opportunity) {
        log.debug("Acknowledged closed opportunity {}", opportunity);
        analyzer.cancelledOpportunity(opportunity);
        xoStatService.ackClosedOpportunity(opportunity);
    }

    private void setThreadNameByOpportunity(FullCrossMarketOpportunity opportunity) {
        Thread.currentThread().setName(String.format(
                "%s %s->%s %s->%s (CURR) %.2f %% @ %.4f",
                opportunity.getUuid(),
                opportunity.getClientFrom(),
                opportunity.getClientTo(),
                opportunity.getCurrencyFrom(),
                opportunity.getCurrencyTo(),
                (opportunity.getHistWin().getCurr() - 1.0) * 100.0,
                opportunity.getMarketFromBestBuyAmount().getCurr()
        ));
    }
}
