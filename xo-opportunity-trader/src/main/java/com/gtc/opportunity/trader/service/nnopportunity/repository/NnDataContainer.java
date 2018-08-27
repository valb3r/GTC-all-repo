package com.gtc.opportunity.trader.service.nnopportunity.repository;

import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.service.dto.FlatOrderBook;
import com.gtc.opportunity.trader.service.dto.FlatOrderBookWithHistory;
import com.gtc.opportunity.trader.service.nnopportunity.dto.Snapshot;
import com.gtc.opportunity.trader.service.nnopportunity.solver.Key;

import java.util.*;
import java.util.stream.Collectors;

public class NnDataContainer {

    private final Key key;
    private final Map<Strategy, StrategyData> strategies;

    NnDataContainer(Key key, NnConfig config) {
        this.key = key;
        this.strategies = Arrays.stream(Strategy.values())
                .collect(Collectors.toMap(it -> it, it -> new StrategyData(it, config)));
    }

    Map<Strategy, FlatOrderBookWithHistory> add(FlatOrderBook book) {
        Map<Strategy, FlatOrderBookWithHistory> result = new EnumMap<>(Strategy.class);
        strategies.forEach((strategy, data) ->
            data.addBook(book).ifPresent(res -> result.put(strategy, res))
        );

        return result;
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

        double minCacheFullness = strategies.values().stream().mapToDouble(StrategyData::fullness).min().orElse(-1.0);
        double minLabelFullness = strategies.values().stream().mapToDouble(it -> Math.min(
                (double) it.getNoopLabel().size() / it.getCollectNlabels(),
                (double) it.getActLabel().size() / it.getCollectNlabels()
        )).min().orElse(0.0);

        return new ContainerStatistics(avgNoopAgeS, avgActAgeS, minCacheFullness, minLabelFullness);
    }

    private double avgAge(Queue<FlatOrderBookWithHistory> data, long currTime) {
        if (data.isEmpty()) {
            return -1.0;
        }

        double total = 0.0;
        for (FlatOrderBookWithHistory book : data) {
            total += (currTime - book.getTimestamp()) / 1000.0;
        }

        return total / data.size();
    }
}
