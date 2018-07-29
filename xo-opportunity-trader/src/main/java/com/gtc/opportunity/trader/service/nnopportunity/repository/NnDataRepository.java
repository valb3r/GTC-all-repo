package com.gtc.opportunity.trader.service.nnopportunity.repository;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.config.NnConfig;
import com.gtc.opportunity.trader.service.nnopportunity.dto.Snapshot;
import com.gtc.opportunity.trader.service.nnopportunity.util.BookFlattener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Valentyn Berezin on 27.07.18.
 */
@Component
@RequiredArgsConstructor
public class NnDataRepository {

    private final Map<String, NnDataContainer> dataStream = new ConcurrentHashMap<>();

    private final NnConfig nnConfig;

    public void addOrderBook(OrderBook orderBook) {
        dataStream.computeIfAbsent(key(orderBook), id -> new NnDataContainer(
                nnConfig.getCollectNlabeled(),
                nnConfig.getNoopThreshold())
        ).add(BookFlattener.simplify(orderBook));
    }

    Optional<Snapshot> getDataToAnalyze(OrderBook orderBook, Strategy strategy) {
        NnDataContainer container = dataStream.get(key(orderBook));
        if (null == container || !container.actReady(strategy) || !container.noopReady(strategy)) {
            return Optional.empty();
        }

        return Optional.of(container.snapshot(strategy));
    }

    private static String key(OrderBook book) {
        return String.format("%s-%s->%s",
                book.getMeta().getClient(),
                book.getMeta().getPair().getFrom().getCode(),
                book.getMeta().getPair().getTo().getCode()
        );
    }
}
