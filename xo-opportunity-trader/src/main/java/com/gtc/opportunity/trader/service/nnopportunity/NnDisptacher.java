package com.gtc.opportunity.trader.service.nnopportunity;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.service.nnopportunity.repository.NnDataRepository;
import com.gtc.opportunity.trader.service.nnopportunity.solver.NnAnalyzer;
import com.gtc.opportunity.trader.service.xoopportunity.creation.ConfigCache;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Service
@RequiredArgsConstructor
public class NnDisptacher {

    private static final double EPSILON = 1e-16;

    private final NnDataRepository repository;
    private final NnAnalyzer analyzer;
    private final ConfigCache cfg;

    @Trace(dispatcher = true)
    public void acceptOrderBook(OrderBook book) {
        if (!validateBook(book) || !cfg.readConfig(book).isPresent()) {
            return;
        }

        try {
            analyzer.analyzeAndCreateTradesIfNecessary(book);
        } finally {
            repository.addOrderBook(book);
        }
    }

    private boolean validateBook(OrderBook book) {
        return Double.isFinite(book.getBestSell())
                && Double.isFinite(book.getBestBuy())
                && book.getBestSell() > EPSILON
                && book.getBestBuy() > EPSILON;
    }
}
