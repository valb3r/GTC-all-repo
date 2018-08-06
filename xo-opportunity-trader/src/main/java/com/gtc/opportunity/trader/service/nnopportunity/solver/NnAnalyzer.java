package com.gtc.opportunity.trader.service.nnopportunity.solver;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.service.nnopportunity.creation.NnCreateTradesService;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Service
@RequiredArgsConstructor
public class NnAnalyzer {

    private final NnSolver solver;
    private final NnCreateTradesService createTradesService;

    @Trace(dispatcher = true)
    public void analyzeAndCreateTradesIfNecessary(OrderBook orderBook) {
        solver.findStrategy(orderBook).ifPresent(strategy -> createTradesService.create(strategy, orderBook));
    }
}
