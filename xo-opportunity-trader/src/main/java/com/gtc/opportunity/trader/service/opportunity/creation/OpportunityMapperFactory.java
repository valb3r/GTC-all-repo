package com.gtc.opportunity.trader.service.opportunity.creation;

import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.cqe.domain.Statistic;
import com.gtc.opportunity.trader.domain.ClientConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by Valentyn Berezin on 24.03.18.
 */
@Service
public class OpportunityMapperFactory {

    public MappedOpp map(FullCrossMarketOpportunity opportunity, ClientConfig from, ClientConfig to) {
        return new MappedOpp(opportunity, from, to);
    }

    @RequiredArgsConstructor
    public static class MappedOpp {

        private final FullCrossMarketOpportunity opportunity;
        private final ClientConfig from;
        private final ClientConfig to;

        public double marketToBestSellPrice() {
            return mapStatistic(opportunity.getMarketToBestSellPrice());
        }

        public double marketToBestSellAmount() {
            return mapStatistic(opportunity.getMarketFromBestBuyAmount());
        }

        public double marketFromBestBuyPrice() {
            return mapStatistic(opportunity.getMarketFromBestBuyPrice());
        }

        public double marketFromBestBuyAmount() {
            return mapStatistic(opportunity.getMarketFromBestBuyAmount());
        }

        public double profitPct() {
            return mapStatistic(opportunity.getHistWin()) * 100.0 - 100.0;
        }

        public FullCrossMarketOpportunity.Histogram[] marketFromBuyHistogram() {
            return opportunity.getBuy();
        }

        public FullCrossMarketOpportunity.Histogram[] marketToSellHistogram() {
            return opportunity.getSell();
        }

        private double mapStatistic(Statistic statistic) {

            return statistic.getCurr();
        }
    }
}
