package com.gtc.opportunity.trader.service.nnopportunity.solver;

import com.google.common.util.concurrent.RateLimiter;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.service.dto.FlatOrderBook;
import com.gtc.opportunity.trader.service.nnopportunity.dto.Snapshot;
import com.gtc.opportunity.trader.service.nnopportunity.repository.NnDataRepository;
import com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy;
import com.gtc.opportunity.trader.service.nnopportunity.solver.model.ModelFactory;
import com.gtc.opportunity.trader.service.nnopportunity.solver.model.NnModelPredict;
import com.gtc.opportunity.trader.service.nnopportunity.util.BookFlattener;
import com.gtc.opportunity.trader.service.nnopportunity.util.OrderBookKey;
import com.gtc.opportunity.trader.service.opportunity.creation.ConfigCache;
import com.newrelic.api.agent.Trace;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy.BUY_LOW_SELL_HIGH;
import static com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy.SELL_HIGH_BUY_LOW;

/**
 * Created by Valentyn Berezin on 27.07.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NnSolver {

    private static final long MILLIS_IN_MINUTE = 60000L;

    private final Map<KeyAndStrategy, NnModelPredict> predictors = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    private final ConfigCache cfgCache;
    private final ModelFactory factory;
    private final NnDataRepository repository;

    Optional<Strategy> findStrategy(OrderBook book) {
        if (solveForStrategy(book, BUY_LOW_SELL_HIGH)) {
            return Optional.of(BUY_LOW_SELL_HIGH);

        } else if (solveForStrategy(book, SELL_HIGH_BUY_LOW)) {
            return Optional.of(SELL_HIGH_BUY_LOW);
        }

        return Optional.empty();
    }

    @Trace(dispatcher = true)
    @Scheduled(fixedDelayString = "#{${app.nn.schedule.createModelsEachS} * 1000}")
    public void createModels() {
        try {
            Arrays.stream(Strategy.values()).forEach(strategy ->
                    repository.getModellable().stream()
                            .map(it -> repository.getDataToAnalyze(it, strategy))
                            .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                            .filter(it -> !oldData(it))
                            .forEach(it -> createPredictor(strategy, it))
            );
        } catch (RuntimeException ex) {
            log.error("Failed creating models", ex);
        }
    }

    private boolean solveForStrategy(OrderBook book, Strategy strategy) {
        NnModelPredict predict = predictors.get(key(OrderBookKey.key(book), strategy));

        if (null == predict) {
            return false;
        }

        if (oldModel(book, predict) || !getLimiter(book).tryAcquire()) {
            return false;
        }

        return predict.canProceed(BookFlattener.simplify(book));
    }

    private void createPredictor(Strategy strategy, Snapshot snapshot) {
        log.info("Create(train) predictor for {}", snapshot.getKey());
        try {
            NnModelPredict predict = factory.buildModel(snapshot);
            predictors.put(key(snapshot.getKey(), strategy), predict);
        } catch (NnModelPredict.TrainingFailed ex) {
            log.warn("Training failed for {}", snapshot.getKey());
        }
    }

    private boolean oldModel(OrderBook book, NnModelPredict predict) {
        return book.getMeta().getTimestamp() - predict.getCreationTimestamp()
                >= cfgCache.requireConfig(book).getOldThresholdM() * MILLIS_IN_MINUTE;
    }

    private boolean oldData(Snapshot snapshot) {
        long actOld = (long) snapshot.getProceedLabel().stream().mapToLong(FlatOrderBook::getTimestamp).average()
                .orElse(0.0);
        long noopOld = (long) snapshot.getNoopLabel().stream().mapToLong(FlatOrderBook::getTimestamp).average()
                .orElse(0.0);

        return System.currentTimeMillis() - Math.min(actOld, noopOld)
                >= cfgCache.requireConfig(snapshot).getOldThresholdM() * MILLIS_IN_MINUTE;
    }

    private RateLimiter getLimiter(OrderBook book) {
        NnConfig config = cfgCache.requireConfig(book);
        return limiters.computeIfAbsent(
                limiterKey(book),
                id -> RateLimiter.create(config.getBookTestForOpenPerS().doubleValue())
        );
    }

    private static String limiterKey(OrderBook book) {
        return book.getMeta().getClient()
                + book.getMeta().getPair().getFrom().getCode()
                + book.getMeta().getPair().getTo().getCode();
    }

    private static KeyAndStrategy key(Key key, Strategy strategy) {
        return new KeyAndStrategy(key, strategy);
    }

    @Data
    @EqualsAndHashCode
    private static class KeyAndStrategy {

        private final Key key;
        private final Strategy strategy;
    }
}
