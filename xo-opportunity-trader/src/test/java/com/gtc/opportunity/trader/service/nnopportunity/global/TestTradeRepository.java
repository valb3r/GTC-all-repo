package com.gtc.opportunity.trader.service.nnopportunity.global;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.gtc.meta.CurrencyPair;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.provider.AggregatedOrder;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.domain.ClientConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 31.07.18.
 */
@Slf4j
@RequiredArgsConstructor
class TestTradeRepository {

    private static final double EPSILON = 1e-16;
    private static final long MILLIS_IN_10M = 600000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, Map<CurrencyPair, List<Opened>>> byClientPairOrders =
            new ConcurrentHashMap<>();

    private final Map<TradingCurrency, BigDecimal> lockedBalance = new ConcurrentHashMap<>();
    private final Map<Boolean, Map<String, Double>> byIsSellByTradeIdDeviation = new ConcurrentHashMap<>();
    private final List<Closed> done = new CopyOnWriteArrayList<>();

    private final Map<String, ClientConfig> configs;
    private final GlobalNnPerformanceTest.EnvContainer envContainer;

    private LocalDateTime min = LocalDateTime.MAX;
    private LocalDateTime max = LocalDateTime.MIN;
    private LocalDateTime current = LocalDateTime.MIN;
    private long pointIndex = 0;

    private static boolean isSell(CreateOrderCommand command) {
        return command.getAmount().compareTo(BigDecimal.ZERO) < 0;
    }

    void acceptTrade(CreateOrderCommand command, long networkLagPts) {
        Map<CurrencyPair, List<Opened>> orders =
                byClientPairOrders.computeIfAbsent(command.getClientName(), id -> new ConcurrentHashMap<>());

        orders.computeIfAbsent(obtainPair(command), id -> new CopyOnWriteArrayList<>())
                .add(new Opened(
                        command.getId(),
                        pointIndex + networkLagPts,
                        current.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli(),
                        command)
                );
    }

    void acceptOrderBook(OrderBook book) {
        if (!Double.isFinite(book.getBestSell())
                || !Double.isFinite(book.getBestBuy())
                || book.getBestSell() < EPSILON
                || book.getBestBuy() < EPSILON) {
            return;
        }

        pointIndex++;
        computeMinMaxDate(book);
        List<Opened> open = byClientPairOrders
                .getOrDefault(book.getMeta().getClient(), ImmutableMap.of())
                .getOrDefault(book.getMeta().getPair(), Collections.emptyList());
        List<Opened> closed = open.stream()
                .filter(it -> it.getMinimalIndexThatCanClose() <= pointIndex)
                .filter(it -> canCompleteCommand(it.getCommand(), book))
                .collect(Collectors.toList());

        computeLockedBalance(book.getMeta().getClient());
        computeTradeDeviations(book);

        if (closed.isEmpty()) {
            return;
        }

        closed.forEach(opn -> log.info("Satisfy(close) {} with {}", opn, book));
        open.removeAll(closed);
        done.addAll(
                closed.stream().map(it -> new Closed(
                        it.getPairId(),
                        it.getTimestamp(),
                        book.getMeta().getTimestamp(),
                        it.getCommand(),
                        book)
                ).collect(Collectors.toList())
        );
    }

    void logStats() {
        long active = byClientPairOrders.entrySet().stream()
                .flatMap(it -> it.getValue().values().stream())
                .mapToLong(Collection::size)
                .sum();

        log.info("In period {} to {} we have active {} / completed {} orders", min, max, active, done.size());

        configs.keySet().forEach(this::computeStatsForClient);
    }

    private void computeStatsForClient(String client) {

        Map<TradingCurrency, BigDecimal> doneBalance = computeOrderBalance(client, done);
        Map<TradingCurrency, BigDecimal> pairwiseDoneBalance = computeOrderBalance(client, computePaired(done));
        Map<CurrencyPair, Map<Boolean, List<Double>>> pairwiseBestAmounts =
                computeClosingAmountsAtBest(computePaired(done));
        Map<Boolean, List<Long>> timeToClose = computeTimeToClose(done, MILLIS_IN_10M);

        log.info("--------------------------- Statistics for {} ------------------------", client);
        reportJsonStats(client, doneBalance, pairwiseDoneBalance, pairwiseBestAmounts, timeToClose);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private void reportJsonStats(
            String client,
            Map<TradingCurrency, BigDecimal> doneBalance,
            Map<TradingCurrency, BigDecimal> pairwiseDoneBalance,
            Map<CurrencyPair, Map<Boolean, List<Double>>> pairwiseBestAmounts,
            Map<Boolean, List<Long>> timeToClose) {

        Supplier<Map<String, Object>> newMap = LinkedHashMap::new;
        Map<String, Object> report = newMap.get();
        Function<String, Map<String, Object>> reportKey = root ->
                (Map<String, Object>) report.computeIfAbsent(root, id -> new LinkedHashMap<String, Object>());

        report.put("active",
                byClientPairOrders.getOrDefault(client, Collections.emptyMap()).entrySet().stream()
                        .mapToInt(it -> it.getValue().size()).sum());
        report.put("done", done.size());
        report.put("client", client);
        report.put("gain", envContainer.getFutureGainPct());
        report.put("threshold", envContainer.getNoopThreshold());
        report.put("chargeRatePct", envContainer.getChargeRatePct());
        report.put("start", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(min));
        report.put("end", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(max));
        doneBalance.forEach((k, v) -> reportKey.apply("totalDone").put(k.getCode(), v));
        lockedBalance.forEach((k, v) -> reportKey.apply("lockMaxOnStat").put(k.getCode(), v));
        pairwiseDoneBalance.forEach((k, v) -> reportKey.apply("balanceChangeByDone").put(k.getCode(), v));
        pairwiseBestAmounts.forEach((pair, vals) -> vals.forEach((k, v) ->
            logSeriesStats(v, reportKey.apply("whenClosingBest" + (k ? "Sell" : "Buy") + "Amount"))
        ));
        timeToClose.forEach((k, v) ->
                logSeriesStats(v, reportKey.apply("timeToClose" + (k ? "Sell" : "Buy")))
        );
        byIsSellByTradeIdDeviation.forEach((k, v) ->
                logSeriesStats(v.values(), reportKey.apply("priceDeviationsPct" + (k ? "Sell" : "Buy")))
        );

        log.info("Porcelain `{}`", MAPPER.writer().writeValueAsString(report));
        log.info("{}", MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }

    private void computeTradeDeviations(OrderBook book) {
        List<Opened> opened = byClientPairOrders.getOrDefault(book.getMeta().getClient(), Collections.emptyMap())
                .getOrDefault(book.getMeta().getPair(), Collections.emptyList());

        for (Opened open : opened) {
            boolean isSell = open.getCommand().getAmount().compareTo(BigDecimal.ZERO) < 0;
            double deviationPrice = isSell ? book.getBestBuy() : book.getBestSell();

            if (deviationPrice == 0.0) {
                continue;
            }

            double deviation = open.getCommand().getPrice().doubleValue() / deviationPrice * 100.0 - 100.0;

            byIsSellByTradeIdDeviation.computeIfAbsent(
                    isSell,
                    id -> new HashMap<>()
            ).compute(open.getPairId(), (id, value) -> {
               if (null == value) {
                   return deviation;
               }

               return Math.max(value, deviation);
            });
        }
    }

    private List<Closed> computePaired(List<Closed> closed) {
        Map<String, Long> pairOccurency = closed.stream().map(Closed::getPairId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return closed.stream()
                .filter(it -> pairOccurency.get(it.getPairId()) == 2)
                .collect(Collectors.toList());
    }

    private Map<Boolean, List<Long>> computeTimeToClose(List<Closed> closed, long threshold) {
        Map<Boolean, List<Long>> timeToClose = new HashMap<>();

        for (Closed val : closed) {
            long dt = val.getTimestampClose() - val.getTimestampOpen();
            if (dt < threshold) {
                continue;
            }

            timeToClose.computeIfAbsent(isSell(val.getCommand()), id -> new ArrayList<>()).add(dt);
        }

        return timeToClose;
    }

    private Map<CurrencyPair, Map<Boolean, List<Double>>> computeClosingAmountsAtBest(List<Closed> closed) {
        Map<CurrencyPair, Map<Boolean, List<Double>>> amounts = new HashMap<>();

        for (Closed val : closed) {
            boolean isSell = isSell(val.getCommand());
            amounts.computeIfAbsent(obtainPair(val.getCommand()), id -> new HashMap<>())
                    .computeIfAbsent(isSell, id -> new ArrayList<>())
                    .add(isSell ? Math.abs(amountAtPos(val.getCloser().getHistogramBuy(), (short) -1)) :
                            Math.abs(amountAtPos(val.getCloser().getHistogramSell(), (short) 1)));
        }

        return amounts;
    }

    private double amountAtPos(AggregatedOrder[] orders, short pos) {
        return Arrays.stream(orders)
                .filter(it -> it.getPosId() == pos)
                .map(AggregatedOrder::getAmount)
                .findFirst()
                .orElse(0.0);
    }

    private CurrencyPair obtainPair(CreateOrderCommand command) {
        return new CurrencyPair(
                TradingCurrency.fromCode(command.getCurrencyFrom()),
                TradingCurrency.fromCode(command.getCurrencyTo())
        );
    }

    private Map<TradingCurrency, BigDecimal> computeOrderBalance(String client, List<Closed> closed) {
        Map<TradingCurrency, BigDecimal> doneBalance = new EnumMap<>(TradingCurrency.class);
        for (Closed val : closed) {
            BigDecimal from;
            BigDecimal to;

            if (isSell(val.getCommand())) {
                from = val.getCommand().getAmount();
                to = val.getCommand().getAmount().abs().multiply(val.getCommand().getPrice())
                        .multiply(getCharge(client));
            } else {
                from = val.getCommand().getAmount().multiply(getCharge(client));
                to = val.getCommand().getAmount().multiply(val.getCommand().getPrice()).negate();
            }

            doneBalance.compute(
                    TradingCurrency.fromCode(val.getCommand().getCurrencyFrom()),
                    (id, bal) -> null == bal ? from : bal.add(from)
            );
            doneBalance.compute(
                    TradingCurrency.fromCode(val.getCommand().getCurrencyTo()),
                    (id, bal) -> null == bal ? to : bal.add(to)
            );
        }

        return doneBalance;
    }

    private void computeLockedBalance(String client) {
        Map<TradingCurrency, BigDecimal> currentLocked = new EnumMap<>(TradingCurrency.class);
        List<Opened> open = byClientPairOrders.getOrDefault(client, ImmutableMap.of()).values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        for (Opened val : open) {
            OrderBalance balance = computeWalletLock(val.getCommand());

            currentLocked.compute(
                    TradingCurrency.fromCode(val.getCommand().getCurrencyFrom()),
                    (id, bal) -> null == bal ? balance.getFrom() : bal.add(balance.getFrom())
            );
            currentLocked.compute(
                    TradingCurrency.fromCode(val.getCommand().getCurrencyTo()),
                    (id, bal) -> null == bal ? balance.getTo() : bal.add(balance.getTo())
            );
        }

        for (TradingCurrency currency : Sets.union(currentLocked.keySet(), lockedBalance.keySet())) {
            lockedBalance.put(currency,
                    currentLocked.getOrDefault(currency, BigDecimal.ZERO).abs()
                            .max(lockedBalance.getOrDefault(currency, BigDecimal.ZERO).abs())
            );
        }
    }

    private OrderBalance computeWalletLock(CreateOrderCommand command) {
        BigDecimal from;
        BigDecimal to;

        if (isSell(command)) {
            from = command.getAmount();
            to = BigDecimal.ZERO;
        } else {
            from = BigDecimal.ZERO;
            to = command.getAmount().multiply(command.getPrice()).negate();
        }

        return new OrderBalance(from, to);
    }

    private <T extends Number> void logSeriesStats(Collection<T> values, Map<String, Object> target) {
        DescriptiveStatistics statistics = new DescriptiveStatistics();
        values.forEach(it -> statistics.addValue(it.doubleValue()));
        target.put("mean", statistics.getMean());
        target.put("stdev", statistics.getStandardDeviation());
        target.put("percentile10", statistics.getPercentile(10.0));
        target.put("percentile25", statistics.getPercentile(25.0));
        target.put("percentile50", statistics.getPercentile(50.0));
        target.put("percentile75", statistics.getPercentile(75.0));
        target.put("percentile90", statistics.getPercentile(90.0));
    }

    private BigDecimal getCharge(String client) {
        return configs.get(client).getTradeChargeRatePct().movePointLeft(2).negate().add(BigDecimal.ONE);
    }

    private boolean canCompleteCommand(CreateOrderCommand command, OrderBook book) {
        if (command.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            return command.getPrice().compareTo(BigDecimal.valueOf(book.getBestBuy())) <= 0;
        } else {
            return command.getPrice().compareTo(BigDecimal.valueOf(book.getBestSell())) >= 0;
        }
    }

    private void computeMinMaxDate(OrderBook book) {
        LocalDateTime time = Instant.ofEpochMilli(book.getMeta().getTimestamp())
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        current = time;
        min = time.compareTo(min) < 0 ? time : min;
        max = time.compareTo(max) > 0 ? time : max;
    }

    @Data
    private static class Closed {

        private final String pairId;
        private final long timestampOpen;
        private final long timestampClose;
        private final CreateOrderCommand command;
        private final OrderBook closer;
    }

    @Data
    private static class Opened {

        private final String pairId;
        private final long minimalIndexThatCanClose;
        private final long timestamp;
        private final CreateOrderCommand command;
    }

    @Data
    private static class OrderBalance {

        private final BigDecimal from;
        private final BigDecimal to;
    }
}
