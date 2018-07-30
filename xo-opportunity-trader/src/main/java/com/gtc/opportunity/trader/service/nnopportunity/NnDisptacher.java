package com.gtc.opportunity.trader.service.nnopportunity;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.config.NnConfig;
import com.gtc.opportunity.trader.service.nnopportunity.repository.NnDataRepository;
import com.gtc.opportunity.trader.service.nnopportunity.solver.NnAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Service
@RequiredArgsConstructor
public class NnDisptacher {

    private final NnDataRepository repository;
    private final NnAnalyzer analyzer;
    private final NnConfig cfg;

    public void acceptOrderBook(OrderBook book) {
        if (!cfg.getEnabledOn().getOrDefault(book.getMeta().getClient(), Collections.emptySet())
                .contains(book.getMeta().getPair())) {
            return;
        }

        analyzer.analyzeAndCreateTradesIfNecessary(book);
        repository.addOrderBook(book);
    }
}
