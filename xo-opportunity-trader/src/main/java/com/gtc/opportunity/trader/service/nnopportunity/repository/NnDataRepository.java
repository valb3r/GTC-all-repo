package com.gtc.opportunity.trader.service.nnopportunity.repository;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.service.nnopportunity.dto.Snapshot;
import com.gtc.opportunity.trader.service.nnopportunity.solver.Key;
import com.gtc.opportunity.trader.service.nnopportunity.util.BookFlattener;
import com.gtc.opportunity.trader.service.opportunity.creation.ConfigCache;
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

    private final Map<Key, NnDataContainer> dataStream = new ConcurrentHashMap<>();
    private final ConfigCache cache;

    public void addOrderBook(OrderBook orderBook) {
        cache.readConfig(orderBook).ifPresent(book ->
            dataStream.computeIfAbsent(key(orderBook), id -> new NnDataContainer(id, book))
                    .add(BookFlattener.simplify(orderBook))
        );
    }

    public Optional<Snapshot> getDataToAnalyze(Key key, Strategy strategy) {
        NnDataContainer container = dataStream.get(key);
        if (null == container || !container.isReady(strategy)) {
            return Optional.empty();
        }

        return Optional.of(container.snapshot(strategy));
    }

    public Set<Key> getModellable() {
        return dataStream.keySet();
    }
}
