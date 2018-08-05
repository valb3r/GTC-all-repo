package com.gtc.opportunity.trader.service.nnopportunity.repository;

import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.service.dto.FlatOrderBook;
import com.gtc.opportunity.trader.service.nnopportunity.dto.Snapshot;
import com.gtc.opportunity.trader.service.nnopportunity.solver.Key;

import java.util.*;
import java.util.stream.Collectors;

public class NnDataContainer {

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

    ContainerStatistics statistics(long currTimeMs) {
        double avgNoopAgeS = strategies.values().stream()
                .mapToDouble(it -> avgAge(it.getNoopLabel(), currTimeMs)).average().orElse(-1.0);
        double avgActAgeS = strategies.values().stream()
                .mapToDouble(it -> avgAge(it.getActLabel(), currTimeMs)).average().orElse(-1.0);

        double minCacheFullness = (double) uncategorized.size() / capacity;
        double minLabelFullness = strategies.values().stream().mapToDouble(it -> Math.min(
                (double) it.getNoopLabel().size() / it.getCollectNlabels(),
                (double) it.getActLabel().size() / it.getCollectNlabels()
        )).min().orElse(0.0);

        return new ContainerStatistics(avgNoopAgeS, avgActAgeS, minCacheFullness, minLabelFullness);
    }

    private double avgAge(Queue<FlatOrderBook> data, long currTime) {
        double total = 0.0;
        for (FlatOrderBook book : data) {
            total += (currTime - book.getTimestamp()) / 1000.0;
        }

        return total / data.size();
    }
}
