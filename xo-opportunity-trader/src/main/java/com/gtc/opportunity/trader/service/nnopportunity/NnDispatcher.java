package com.gtc.opportunity.trader.service.nnopportunity;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.service.dto.FlatOrderBookWithHistory;
import com.gtc.opportunity.trader.service.nnopportunity.repository.NnDataRepository;
import com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy;
import com.gtc.opportunity.trader.service.nnopportunity.solver.NnAnalyzer;
import com.gtc.opportunity.trader.service.xoopportunity.creation.ConfigCache;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Service
@RequiredArgsConstructor
public class NnDispatcher {

    private static final double MAX_VAL = 1e10;
    private static final double EPSILON = 1e-16;

    private final NnDataRepository repository;
    private final NnAnalyzer analyzer;
    private final ConfigCache cfg;

    @Trace(dispatcher = true)
    public void acceptOrderBook(OrderBook book) {
        if (!validateBook(book) || !cfg.readConfig(book).isPresent()) {
            return;
        }

        Map<Strategy, FlatOrderBookWithHistory> enhancedBook = repository.addOrderBook(book);
        analyzer.analyzeAndCreateTradesIfNecessary(book, enhancedBook);
    }

    private boolean validateBook(OrderBook book) {
        return Double.isFinite(book.getBestSell())
                && Double.isFinite(book.getBestBuy())
                && book.getBestSell() > EPSILON
                && book.getBestBuy() > EPSILON
                && book.getBestBuy() < MAX_VAL
                && book.getBestSell() < MAX_VAL;
    }
}
