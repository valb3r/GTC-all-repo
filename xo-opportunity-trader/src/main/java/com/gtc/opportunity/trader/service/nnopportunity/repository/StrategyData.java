package com.gtc.opportunity.trader.service.nnopportunity.repository;

import com.google.common.collect.EvictingQueue;
import com.gtc.opportunity.trader.service.dto.FlatOrderBook;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
class StrategyData {

    private final Map<Float, AtomicInteger> pricesInFuture = new HashMap<>();

    @Getter
    private final Queue<FlatOrderBook> noopLabel;

    @Getter
    private final Queue<FlatOrderBook> actLabel;

    private final float gainNoopThreshold;
    private final Function<FlatOrderBook, Float> getFirstPrice;
    private final Function<FlatOrderBook, Float> getSecondPrice;
    private final Function<Stream<Float>, Float> futurePriceDesiredBound;

    StrategyData(Strategy strategy, int capacity, float gainNoopThreshold) {
        this.gainNoopThreshold = gainNoopThreshold;

        noopLabel = EvictingQueue.create(capacity);
        actLabel = EvictingQueue.create(capacity);

        if (strategy == Strategy.BUY_LOW_SELL_HIGH) {
            getFirstPrice = FlatOrderBook::getBestSell;
            getSecondPrice = FlatOrderBook::getBestBuy;
            futurePriceDesiredBound = vals -> vals.max(Float::compareTo).orElse(0.0f);
        } else if (strategy == Strategy.SELL_HIGH_BUY_LOW) {
            getFirstPrice = FlatOrderBook::getBestBuy;
            getSecondPrice = FlatOrderBook::getBestSell;
            futurePriceDesiredBound = vals -> vals.min(Float::compareTo).orElse(0.0f);
        } else {
            throw new IllegalStateException("Unknown strategy " + strategy.name());
        }
    }

    void addPrice(FlatOrderBook book) {
        synchronized (pricesInFuture) {
            pricesInFuture.computeIfAbsent(getSecondPrice.apply(book), id -> new AtomicInteger()).incrementAndGet();
        }
    }

    void evictPrice(FlatOrderBook book) {
        float price = getSecondPrice.apply(book);
        synchronized (pricesInFuture) {
            if (0 == pricesInFuture.get(price).decrementAndGet()) {
                pricesInFuture.remove(price);
            }
        }
    }

    void labelAndStore(FlatOrderBook book) {
        Set<Float> futurePrices;
        synchronized (pricesInFuture) {
            futurePrices = new HashSet<>(pricesInFuture.keySet());
        }

        float bestFuture = futurePriceDesiredBound.apply(futurePrices.stream());
        if (bestFuture / getFirstPrice.apply(book) <= gainNoopThreshold) {
            synchronized (noopLabel) {
                noopLabel.add(book);
            }
        } else {
            synchronized (actLabel) {
                actLabel.add(book);
            }
        }
    }
}
