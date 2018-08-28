package com.gtc.opportunity.trader.service.nnopportunity.repository;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.service.dto.FlatOrderBookWithHistory;
import com.gtc.opportunity.trader.service.nnopportunity.dto.Snapshot;
import com.gtc.opportunity.trader.service.nnopportunity.solver.Key;
import com.gtc.opportunity.trader.service.nnopportunity.util.BookFlattener;
import com.gtc.opportunity.trader.service.xoopportunity.creation.ConfigCache;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.gtc.opportunity.trader.service.nnopportunity.util.OrderBookKey.key;

/**
 * Created by Valentyn Berezin on 27.07.18.
 */
@Component
@RequiredArgsConstructor
public class NnDataRepository {

    private final Map<Key, VersionedDataContainer> dataStream = new ConcurrentHashMap<>();
    private final ConfigCache cache;

    public Map<Strategy, FlatOrderBookWithHistory> addOrderBook(OrderBook orderBook) {
        Optional<NnConfig> cfg = cache.readConfig(orderBook);

        if (!cfg.isPresent()) {
            return Collections.emptyMap();
        }

        return dataStream.compute(
                key(orderBook),
                (id, value) ->
                        (null == value || value.getHashVer() != cfg.get().modelHashValue()) ?
                                new VersionedDataContainer(cfg.get().modelHashValue(), new NnDataContainer(id, cfg.get()))
                                : value
        ).getData().add(BookFlattener.simplify(orderBook));
    }

    public Optional<Snapshot> getDataToAnalyze(Key key, Strategy strategy) {
        VersionedDataContainer container = dataStream.get(key);
        if (null == container || !container.getData().isReady(strategy)) {
            return Optional.empty();
        }

        return Optional.of(container.getData().snapshot(strategy));
    }

    public List<ContainerStatistics> getStatistics(long currentTime) {
        return dataStream.values().stream()
                .map(VersionedDataContainer::getData)
                .map(it -> it.statistics(currentTime))
                .collect(Collectors.toList());
    }

    public Set<Key> getModellable() {
        return dataStream.keySet();
    }

    @Data
    private static class VersionedDataContainer {

        private final int hashVer;
        private final NnDataContainer data;
    }
}
