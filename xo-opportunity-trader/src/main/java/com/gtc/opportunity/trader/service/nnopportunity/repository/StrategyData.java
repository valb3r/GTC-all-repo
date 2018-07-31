package com.gtc.opportunity.trader.service.nnopportunity.repository;

import com.google.common.collect.EvictingQueue;
import com.gtc.opportunity.trader.config.NnConfig;
import com.gtc.opportunity.trader.service.dto.FlatOrderBook;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Slf4j
class StrategyData {

    private final Map<Float, AtomicInteger> pricesInFuture = new HashMap<>();

    private final AtomicLong lastBookTimestamp = new AtomicLong();

    @Getter
    private final Queue<FlatOrderBook> noopLabel;

    @Getter
    private final Queue<FlatOrderBook> actLabel;

    private final NnConfig cfg;
    private final float gainNoopThreshold;
    private final Function<FlatOrderBook, Float> getFirstPrice;
    private final Function<FlatOrderBook, Float> getSecondPrice;
    private final BiFunction<Float, Float, Float> gainOnFirstSecond;
    private final Function<Stream<Float>, Float> futurePriceDesiredBound;

    StrategyData(Strategy strategy, NnConfig config) {
        this.cfg = config;
        this.gainNoopThreshold = config.getNoopThreshold();

        noopLabel = EvictingQueue.create(config.getCollectNlabeled());
        actLabel = EvictingQueue.create(config.getCollectNlabeled());

        if (strategy == Strategy.BUY_LOW_SELL_HIGH) {
            getFirstPrice = FlatOrderBook::getBestSell;
            getSecondPrice = FlatOrderBook::getBestBuy;
            futurePriceDesiredBound = vals -> vals.max(Float::compareTo).orElse(0.0f);
            gainOnFirstSecond = (first, second) -> second / first;
        } else if (strategy == Strategy.SELL_HIGH_BUY_LOW) {
            getFirstPrice = FlatOrderBook::getBestBuy;
            getSecondPrice = FlatOrderBook::getBestSell;
            futurePriceDesiredBound = vals -> vals.min(Float::compareTo).orElse(0.0f);
            gainOnFirstSecond = (first, second) -> first / second;
        } else {
            throw new IllegalStateException("Unknown strategy " + strategy.name());
        }
    }

    boolean isReady() {
        return cfg.getCollectNlabeled() == noopLabel.size() && cfg.getCollectNlabeled() == actLabel.size();
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

    void labelIfCompliantAndStore(FlatOrderBook book) {
        if (!canStore(book)) {
            return;
        }

        lastBookTimestamp.set(book.getTimestamp());

        Set<Float> futurePrices;
        synchronized (pricesInFuture) {
            futurePrices = new HashSet<>(pricesInFuture.keySet());
        }

        float bestFuture = futurePriceDesiredBound.apply(futurePrices.stream());
        if (gainOnFirstSecond.apply(getFirstPrice.apply(book), bestFuture) <= gainNoopThreshold) {
            synchronized (noopLabel) {
                noopLabel.add(book);
            }
        } else {
            synchronized (actLabel) {
                actLabel.add(book);
            }
        }
    }

    private boolean canStore(FlatOrderBook book) {
        long lastTimestamp = lastBookTimestamp.get();
        return book.getTimestamp() - lastTimestamp >= cfg.getAverageDtSBetweenLabels() * 1000;
    }
}
