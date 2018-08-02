package com.gtc.opportunity.trader.service.nnopportunity;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.service.nnopportunity.repository.NnDataRepository;
import com.gtc.opportunity.trader.service.nnopportunity.solver.NnAnalyzer;
import com.gtc.opportunity.trader.service.opportunity.creation.ConfigCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Service
@RequiredArgsConstructor
public class NnDisptacher {

    private final NnDataRepository repository;
    private final NnAnalyzer analyzer;
    private final ConfigCache cfg;

    public void acceptOrderBook(OrderBook book) {
        if (!cfg.readConfig(book).isPresent()) {
            return;
        }

        analyzer.analyzeAndCreateTradesIfNecessary(book);
        repository.addOrderBook(book);
    }
}
