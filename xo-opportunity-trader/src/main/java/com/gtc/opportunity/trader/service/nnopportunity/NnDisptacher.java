package com.gtc.opportunity.trader.service.nnopportunity;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.service.nnopportunity.repository.NnDataRepository;
import com.gtc.opportunity.trader.service.nnopportunity.solver.NnAnalyzer;
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

    public void acceptOrderBook(OrderBook book) {
        analyzer.analyzeAndCreateTradesIfNecessary(book);
        repository.addOrderBook(book);
    }
}
