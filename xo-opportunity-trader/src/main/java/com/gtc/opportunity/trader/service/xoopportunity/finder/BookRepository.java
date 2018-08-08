package com.gtc.opportunity.trader.service.xoopportunity.finder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.TransactionalIndexedCollection;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.support.CloseableIterator;
import com.googlecode.cqengine.index.unique.UniqueIndex;
import com.googlecode.cqengine.resultset.ResultSet;
import com.gtc.meta.CurrencyPair;
import com.gtc.model.provider.AggregatedOrder;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.cqe.domain.CrossMarketOpportunity;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.cqe.domain.IndexedOrderBook;
import com.gtc.opportunity.trader.cqe.domain.Statistic;
import com.gtc.opportunity.trader.domain.XoConfig;
import com.gtc.opportunity.trader.service.xoopportunity.creation.ConfigCache;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.googlecode.cqengine.query.QueryFactory.*;

/**
 * Created by Valentyn Berezin on 16.06.18.
 */
@Component
public class BookRepository {

    private static final int DEFAULT_EXPIRY_MS = 1000;

    private final HashIndex<CurrencyPair, IndexedOrderBook> currencyPairIndex;
    private final IndexedCollection<IndexedOrderBook> books;
    private final ConfigCache cfgCache;

    private final Cache<String, Integer> bookExpiryThreshold = CacheBuilder.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    public BookRepository(ConfigCache cfgCache) {
        this.cfgCache = cfgCache;
        this.books = new TransactionalIndexedCollection<>(IndexedOrderBook.class);
        this.books.addIndex(UniqueIndex.onAttribute((IndexedOrderBook.A_ID)));
        currencyPairIndex = HashIndex.onAttribute(IndexedOrderBook.CURRENCY_PAIR);
        this.books.addIndex(currencyPairIndex);
        this.books.addIndex(HashIndex.onAttribute(IndexedOrderBook.CLIENT));
        this.books.addIndex(NavigableIndex.onAttribute(IndexedOrderBook.BEST_BUY));
        this.books.addIndex(NavigableIndex.onAttribute(IndexedOrderBook.BEST_SELL));
    }

    public void addOrderBook(OrderBook orderBook) {
        IndexedOrderBook newIndexed = new IndexedOrderBook(IndexedOrderBook.calculateId(orderBook), orderBook);

        Optional<IndexedOrderBook> toRemove = Optional.empty();
        try (ResultSet<IndexedOrderBook> res = books.retrieve(equal(IndexedOrderBook.A_ID, newIndexed.getId()))) {
            if (res.isNotEmpty()) {
                toRemove = Optional.of(res.uniqueResult());
            }
        }

        toRemove.ifPresent(books::remove);
        books.add(newIndexed);
    }

    public Set<FullCrossMarketOpportunity> findOpportunities() {
        Set<FullCrossMarketOpportunity> result = new HashSet<>();

        try (CloseableIterator<CurrencyPair> pair = currencyPairIndex.getDistinctKeys(noQueryOptions()).iterator()) {
            while (pair.hasNext()) {
                result.addAll(findOpportunitiesForPair(pair.next()));
            }
        }

        return result;
    }

    private Set<FullCrossMarketOpportunity> findOpportunitiesForPair(CurrencyPair pair) {
        Set<FullCrossMarketOpportunity> result = new HashSet<>();
        long timestamp = System.currentTimeMillis();
        int expiryMax =
                bookExpiryThreshold.asMap().values().stream().mapToInt(it -> it).max().orElse(DEFAULT_EXPIRY_MS);
        long notOlderThan = timestamp - expiryMax;
        List<IndexedOrderBook> forPair = findByPair(pair, notOlderThan);
        for (IndexedOrderBook candidateFrom : forPair) {
            if (checkExpired(candidateFrom, timestamp, expiryMax)) {
                continue;
            }

            for (IndexedOrderBook candidateTo : forPair) {
                if (candidateTo.equals(candidateFrom) || checkExpired(candidateTo, timestamp, expiryMax)) {
                    continue;
                }

                result.add(buildCrossMarketOpportunity(candidateFrom, candidateTo));
            }
        }

        return result;
    }

    @SneakyThrows
    private boolean checkExpired(IndexedOrderBook orderBook, long timestamp, int defaultExpiry) {
        long ttl = bookExpiryThreshold.get(
                orderBook.getMeta().getClient(),
                () -> cfgCache.getXoCfg(
                        orderBook.getMeta().getClient(),
                        orderBook.getMeta().getPair().getFrom(),
                        orderBook.getMeta().getPair().getTo()
                ).map(XoConfig::getStaleBookThresholdMS).orElse(defaultExpiry)
        );
        return orderBook.getRecordedOn() < timestamp - ttl;
    }

    private List<IndexedOrderBook> findByPair(CurrencyPair pair, long notOlderThan) {
        try (ResultSet<IndexedOrderBook> res = books.retrieve(and(
                equal(IndexedOrderBook.CURRENCY_PAIR, pair),
                greaterThanOrEqualTo(IndexedOrderBook.REC_ON, notOlderThan)))) {
            return StreamSupport.stream(res.spliterator(), false).collect(Collectors.toList());
        }
    }

    private FullCrossMarketOpportunity buildCrossMarketOpportunity(IndexedOrderBook from, IndexedOrderBook to) {
        CrossMarketOpportunity xo = CrossMarketOpportunity.builder()
                .uuid(UUID.randomUUID().toString())
                .clientFrom(from.getMeta().getClient())
                .clientTo(to.getMeta().getClient())
                .currencyFrom(from.getMeta().getPair().getFrom())
                .currencyTo(to.getMeta().getPair().getTo())
                .histWin(new Statistic(from.getBestBuy() / to.getBestSell()))
                .marketFromBestBuyAmount(new Statistic(from.getAmountBestBuy()))
                .marketFromBestBuyPrice(new Statistic(from.getBestBuy()))
                .marketToBestSellAmount(new Statistic(to.getAmountBestSell()))
                .marketToBestSellPrice(new Statistic(to.getBestSell()))
                .openedOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .eventCount(1)
                .build();

        return new FullCrossMarketOpportunity(
                FullCrossMarketOpportunity.calculateId(xo),
                xo,
                buildHistogram(to.getHistogramSell()),
                buildHistogram(from.getHistogramBuy())
        );
    }

    private static FullCrossMarketOpportunity.Histogram[] buildHistogram(AggregatedOrder[] orders) {
        return Arrays.stream(orders)
                .map(it -> new FullCrossMarketOpportunity.Histogram(it.getMinPrice(), it.getMaxPrice(), it.getAmount()))
                .toArray(FullCrossMarketOpportunity.Histogram[]::new);
    }
}
