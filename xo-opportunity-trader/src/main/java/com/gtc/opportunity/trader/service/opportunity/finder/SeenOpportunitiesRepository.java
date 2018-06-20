package com.gtc.opportunity.trader.service.opportunity.finder;

import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.TransactionalIndexedCollection;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.resultset.ResultSet;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Optional;

import static com.googlecode.cqengine.query.QueryFactory.equal;

/**
 * Created by Valentyn Berezin on 18.06.18.
 */
@Component
public class SeenOpportunitiesRepository {

    private final IndexedCollection<FullCrossMarketOpportunity> opportunities;

    public SeenOpportunitiesRepository() {
        this.opportunities = new TransactionalIndexedCollection<>(FullCrossMarketOpportunity.class);
        opportunities.addIndex(HashIndex.onAttribute(FullCrossMarketOpportunity.A_ID));
    }

    public FullCrossMarketOpportunity addOrUpdateOpportunity(FullCrossMarketOpportunity opp) {
        Optional<FullCrossMarketOpportunity> existing = findOpportunityById(opp.getId());
        FullCrossMarketOpportunity toAdd = opp;
        if (existing.isPresent()) {
            opportunities.remove(existing.get());
            toAdd = joinOpportunities(opp, existing.get(), false);
        }

        opportunities.add(toAdd);
        return toAdd;
    }

    public Optional<FullCrossMarketOpportunity> removeOpportunity(FullCrossMarketOpportunity opp) {
        Optional<FullCrossMarketOpportunity> oldOpp = findOpportunityById(opp.getId());
        if (oldOpp.isPresent()) {
            opportunities.remove(oldOpp.get());
            return Optional.of(joinOpportunities(opp, oldOpp.get(), true));
        }

        return Optional.empty();
    }

    public Optional<FullCrossMarketOpportunity> findOpportunityById(String id) {
        try (ResultSet<FullCrossMarketOpportunity> opp =
                     opportunities.retrieve(equal(FullCrossMarketOpportunity.A_ID, id))) {
            Iterator<FullCrossMarketOpportunity> iter = opp.iterator();
            return iter.hasNext() ? Optional.of(iter.next()) : Optional.empty();
        }
    }

    private FullCrossMarketOpportunity joinOpportunities(
            FullCrossMarketOpportunity newOpp, FullCrossMarketOpportunity oldOpp, boolean doClose) {

        newOpp.getHistWin().mergeAsPrimary(oldOpp.getHistWin());
        newOpp.getMarketFromBestBuyPrice().mergeAsPrimary(oldOpp.getMarketFromBestBuyPrice());
        newOpp.getMarketFromBestBuyAmount().mergeAsPrimary(oldOpp.getMarketFromBestBuyAmount());
        newOpp.getMarketToBestSellPrice().mergeAsPrimary(oldOpp.getMarketToBestSellPrice());
        newOpp.getMarketToBestSellAmount().mergeAsPrimary(oldOpp.getMarketToBestSellAmount());
        newOpp.setEventCount(newOpp.getEventCount() + oldOpp.getEventCount());

        if (doClose) {
            newOpp.setClosedOn(LocalDateTime.now());
            newOpp.setClosed(true);
        } else {
            newOpp.setUpdatedOn(LocalDateTime.now());
        }

        return newOpp;
    }
}
