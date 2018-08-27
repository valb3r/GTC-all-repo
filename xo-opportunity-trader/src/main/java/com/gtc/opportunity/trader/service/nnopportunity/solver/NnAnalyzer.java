package com.gtc.opportunity.trader.service.nnopportunity.solver;

import com.google.common.base.Throwables;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.service.dto.FlatOrderBookWithHistory;
import com.gtc.opportunity.trader.service.nnopportunity.creation.NnCreateTradesService;
import com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.RejectionException;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NnAnalyzer {

    private final NnSolver solver;
    private final NnCreateTradesService createTradesService;

    @Trace(dispatcher = true)
    public void analyzeAndCreateTradesIfNecessary(
            OrderBook orderBook, Map<Strategy, FlatOrderBookWithHistory> candidates) {
        if (candidates.isEmpty()) {
            return;
        }

        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(String.format(
                "Analyze model %s-%s->%s",
                orderBook.getMeta().getClient(),
                orderBook.getMeta().getPair().getFrom().getCode(),
                orderBook.getMeta().getPair().getTo().getCode()));
        try {
            solver.findStrategy(orderBook, candidates)
                    .ifPresent(strategy -> createTradesService.create(strategy, orderBook));
        } catch (RejectionException ex) {
            log.info("Rejected {}", Throwables.getRootCause(ex).getMessage(), ex);
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }
}
