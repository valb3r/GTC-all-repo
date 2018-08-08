package com.gtc.opportunity.trader.service.xoopportunity.finder;

import com.gtc.opportunity.trader.config.OpportunityConfig;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Created by Valentyn Berezin on 18.06.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunitySearcher {

    private final SeenOpportunitiesRepository opportunitiesRepository;
    private final OpportunityConfig config;
    private final BookRepository bookRepository;
    private final OpportunityAcceptor acceptor;

    @Scheduled(fixedDelayString = "${app.schedule.opportunitySearchMs}")
    public void searchForOpportunities() {
        Set<FullCrossMarketOpportunity> opportunities = bookRepository.findOpportunities();

        opportunities.stream()
                .filter(it -> it.getHistWin().getCurr() >= config.getMinGain())
                .forEach(it -> {
                    FullCrossMarketOpportunity stored = opportunitiesRepository.addOrUpdateOpportunity(it);
                    acceptor.ackCreateOrOpenOpportunity(stored);
                });

        opportunities.stream()
                .filter(it -> it.getHistWin().getCurr() < config.getMinGain())
                .filter(it -> opportunitiesRepository.findOpportunityById(it.getId()).isPresent())
                .forEach(it -> {
                    Optional<FullCrossMarketOpportunity> finalOpp = opportunitiesRepository.removeOpportunity(it);
                    finalOpp.ifPresent(op -> acceptor.ackCloseOpportunity(op.getOpportunity()));
                });
    }
}
