package com.gtc.opportunity.trader.service.nnopportunity.repository;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.service.dto.FlatOrderBook;
import com.gtc.opportunity.trader.service.dto.FlatOrderBookWithHistory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Slf4j
class StrategyData {

    private final AtomicLong lastBookTimestamp = new AtomicLong();
    private final List<FlatOrderBook> booksInFuture = new ArrayList<>();

    @Getter
    private final Queue<FlatOrderBookWithHistory> noopLabel;

    @Getter
    private final Queue<FlatOrderBookWithHistory> actLabel;

    private final NnConfig cfg;
    private final float gainNoopThreshold;
    private final Function<FlatOrderBook, Float> getFirstPrice;
    private final Function<FlatOrderBook, Float> getSecondPrice;
    private final BiFunction<Float, Float, Float> gainOnFirstSecond;
    private final Function<Stream<Float>, Float> futurePriceDesiredBound;

    @Getter
    private final int collectNlabels;

    StrategyData(Strategy strategy, NnConfig config) {
        this.cfg = config;
        this.gainNoopThreshold = config.getNoopThreshold().floatValue();

        this.collectNlabels = cfg.getCollectNlabeled();
        noopLabel = EvictingQueue.create(collectNlabels);
        actLabel = EvictingQueue.create(collectNlabels);

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

    double fullness() {
        return (double) booksInFuture.size() / cfg.getFutureNwindow();
    }

    Optional<FlatOrderBookWithHistory> addBook(FlatOrderBook book) {
        synchronized (booksInFuture) {
            booksInFuture.add(book);
            if (booksInFuture.size() < cfg.getFutureNwindow()) {
                return Optional.empty();
            }
            FlatOrderBook mature = booksInFuture.remove(0);
            return labelIfCompliantAndStore(mature);
        }
    }

    private Optional<FlatOrderBookWithHistory> labelIfCompliantAndStore(FlatOrderBook book) {
        if (!canStore(book)) {
            return Optional.empty();
        }

        lastBookTimestamp.set(book.getTimestamp());

        Set<Float> futurePrices = booksInFuture.stream()
                .map(getSecondPrice)
                .collect(Collectors.toSet());

        float bestFuture = futurePriceDesiredBound.apply(futurePrices.stream());
        FlatOrderBookWithHistory result = new FlatOrderBookWithHistory(book, readCurrentHistory());
        if (gainOnFirstSecond.apply(getFirstPrice.apply(book), bestFuture) <= gainNoopThreshold) {
            synchronized (noopLabel) {
                noopLabel.add(result);
            }
        } else {
            synchronized (actLabel) {
                actLabel.add(result);
            }
        }

        return Optional.of(result);
    }

    private List<FlatOrderBook> readCurrentHistory() {
        return ImmutableList.of(
                getAt(10.0),
                getAt(50.0),
                getAt(100.0)
        );
    }

    private FlatOrderBook getAt(double percentile) {
        int pos = (int) ((booksInFuture.size() - 1) * percentile / 100.0);
        return booksInFuture.get(pos >= booksInFuture.size() ? booksInFuture.size() - 1 : pos);
    }

    private boolean canStore(FlatOrderBook book) {
        long lastTimestamp = lastBookTimestamp.get();
        return book.getTimestamp() - lastTimestamp >= cfg.getAverageDtSBetweenLabels().doubleValue() * 1000;
    }
}
