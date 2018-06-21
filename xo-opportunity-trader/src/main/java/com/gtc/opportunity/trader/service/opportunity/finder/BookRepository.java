package com.gtc.opportunity.trader.service.opportunity.finder;

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
import com.gtc.opportunity.trader.config.OpportunityConfig;
import com.gtc.opportunity.trader.cqe.domain.CrossMarketOpportunity;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.cqe.domain.IndexedOrderBook;
import com.gtc.opportunity.trader.cqe.domain.Statistic;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.googlecode.cqengine.query.QueryFactory.*;

/**
 * Created by Valentyn Berezin on 16.06.18.
 */
@Component
public class BookRepository {

    private final OpportunityConfig cfg;
    private final HashIndex<CurrencyPair, IndexedOrderBook> currencyPairIndex;
    private final IndexedCollection<IndexedOrderBook> books;

    public BookRepository(OpportunityConfig cfg) {
        this.cfg = cfg;
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
        List<IndexedOrderBook> forPair = findByPair(pair);
        for (IndexedOrderBook candidateFrom : forPair) {
            for (IndexedOrderBook candidateTo : forPair) {
                if (candidateTo.equals(candidateFrom)) {
                    continue;
                }

                result.add(buildCrossMarketOpportunity(candidateFrom, candidateTo));
            }
        }

        return result;
    }

    private List<IndexedOrderBook> findByPair(CurrencyPair pair) {
        long notOlderThan = System.currentTimeMillis() - cfg.getIgnoreIfOlderMs();
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
