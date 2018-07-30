package com.gtc.opportunity.trader.service.nnopportunity.repository;

import com.gtc.opportunity.trader.config.NnConfig;
import com.gtc.opportunity.trader.service.dto.FlatOrderBook;
import com.gtc.opportunity.trader.service.nnopportunity.dto.Snapshot;
import com.gtc.opportunity.trader.service.nnopportunity.solver.Key;

import java.util.*;
import java.util.stream.Collectors;

class NnDataContainer {

    private final Key key;
    private final List<FlatOrderBook> uncategorized = new LinkedList<>();
    private final Map<Strategy, StrategyData> strategies;

    private final int capacity;

    NnDataContainer(Key key, NnConfig config) {
        this.key = key;
        this.capacity = config.getFutureNwindow();
        this.strategies = Arrays.stream(Strategy.values())
                .collect(Collectors.toMap(it -> it, it -> new StrategyData(it, config)));
    }

    void add(FlatOrderBook book) {
        if (uncategorized.size() + 1 >= capacity) {
            FlatOrderBook mature = uncategorized.remove(0);
            strategies.forEach((name, data) -> {
                data.evictPrice(mature);
                data.labelIfCompliantAndStore(mature);
            });
        }

        uncategorized.add(book);
        strategies.forEach((name, data) -> data.addPrice(book));
    }

    Snapshot snapshot(Strategy strategy) {
        StrategyData data = strategies.get(strategy);
        return new Snapshot(key, new ArrayList<>(data.getNoopLabel()), new ArrayList<>(data.getActLabel()));
    }

    boolean isReady(Strategy strategy) {
        StrategyData data = strategies.get(strategy);
        return data.isReady();
    }
}
