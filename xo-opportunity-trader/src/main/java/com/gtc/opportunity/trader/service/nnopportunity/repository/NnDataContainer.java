package com.gtc.opportunity.trader.service.nnopportunity.repository;

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

    NnDataContainer(Key key, int capacity, float gainNoopThreshold) {
        this.key = key;
        this.capacity = capacity;
        this.strategies = Arrays.stream(Strategy.values())
                .collect(Collectors.toMap(it -> it, it -> new StrategyData(it, capacity, gainNoopThreshold)));
    }

    void add(FlatOrderBook book) {
        if (uncategorized.size() + 1 >= capacity) {
            FlatOrderBook mature = uncategorized.remove(0);
            strategies.forEach((name, data) -> {
                data.evictPrice(mature);
                data.labelAndStore(mature);
            });
        }

        uncategorized.add(book);
        strategies.forEach((name, data) -> data.addPrice(book));
    }

    Snapshot snapshot(Strategy strategy) {
        StrategyData data = strategies.get(strategy);
        return new Snapshot(key, new ArrayList<>(data.getNoopLabel()), new ArrayList<>(data.getActLabel()));
    }

    boolean noopReady(Strategy strategy) {
        StrategyData data = strategies.get(strategy);
        return capacity == data.getNoopLabel().size();
    }

    boolean actReady(Strategy strategy) {
        StrategyData data = strategies.get(strategy);
        return capacity == data.getActLabel().size();
    }
}
