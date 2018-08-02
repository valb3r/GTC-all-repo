package com.gtc.opportunity.trader.service.nnopportunity.repository;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.service.nnopportunity.dto.Snapshot;
import com.gtc.opportunity.trader.service.nnopportunity.solver.Key;
import com.gtc.opportunity.trader.service.nnopportunity.util.BookFlattener;
import com.gtc.opportunity.trader.service.opportunity.creation.ConfigCache;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.gtc.opportunity.trader.service.nnopportunity.util.OrderBookKey.key;

/**
 * Created by Valentyn Berezin on 27.07.18.
 */
@Component
@RequiredArgsConstructor
public class NnDataRepository {

    private final Map<Key, VersionedDataContainer> dataStream = new ConcurrentHashMap<>();
    private final ConfigCache cache;

    public void addOrderBook(OrderBook orderBook) {
        Optional<NnConfig> cfg = cache.readConfig(orderBook);

        if (!cfg.isPresent()) {
            return;
        }

        dataStream.compute(
                key(orderBook),
                (id, value) ->
                        (null == value || value.getHashVer() != cfg.get().hashValue()) ?
                                new VersionedDataContainer(cfg.get().hashValue(), new NnDataContainer(id, cfg.get()))
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

    public Set<Key> getModellable() {
        return dataStream.keySet();
    }

    @Data
    private static class VersionedDataContainer {

        private final int hashVer;
        private final NnDataContainer data;
    }
}
