package com.gtc.opportunity.trader.service.nnopportunity.global;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.gtc.meta.CurrencyPair;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.command.manage.CancelOrderCommand;
import com.gtc.model.gateway.command.manage.GetOrderCommand;
import com.gtc.model.gateway.command.manage.ListOpenCommand;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.model.gateway.data.OrderStatus;
import com.gtc.model.gateway.response.manage.GetOrderResponse;
import com.gtc.model.gateway.response.manage.ListOpenOrdersResponse;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 31.07.18.
 */
@Slf4j
@RequiredArgsConstructor
class TestTradeRepository {

    private static final double MAX_VAL = 1e10;
    private static final double EPSILON = 1e-16;
    private static final long MILLIS_IN_10M = 600000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, Opened> byIdOrders = new ConcurrentHashMap<>();

    private final Map<TradingCurrency, BigDecimal> lockedBalance = new ConcurrentHashMap<>();
    private final Map<Boolean, Map<String, Double>> byIsSellByTradeIdDeviation = new ConcurrentHashMap<>();
    private final Map<String, Closed> done = new ConcurrentHashMap<>();
    private final Map<String, Closed> cancelled = new ConcurrentHashMap<>();

    private LocalDateTime min = LocalDateTime.MAX;
    private LocalDateTime max = LocalDateTime.MIN;
    private LocalDateTime current = LocalDateTime.MIN;
    private long currentTimestamp = 0;
    private long pointIndex = 0;

    private final ClientConfig config;
    private final GlobalNnPerformanceTest.EnvContainer envContainer;
    private final String clientName;
    private final TradingCurrency from;
    private final TradingCurrency to;
    private final BigDecimal fromBal;
    private final BigDecimal toBal;

    private static boolean isSell(CreateOrderCommand command) {
        return command.getAmount().compareTo(BigDecimal.ZERO) < 0;
    }

    void acceptTrade(CreateOrderCommand command, long networkLagPts) {
        if (!command.getClientName().equals(clientName) || !from.getCode().equals(command.getCurrencyFrom())
                || !to.getCode().equals(command.getCurrencyTo())) {
            return;
        }

        Map<String, BigDecimal> balance = balances();

        if (command.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            if (balance.get(from.getCode()).add(command.getAmount()).compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Aborted (from) due to low balance {}", command);
                return;
            }
        } else {
            if (balance.get(to.getCode()).subtract(command.getAmount().multiply(command.getPrice()))
                    .compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Aborted (to) due to low balance {}", command);
                return;
            }
        }

        byIdOrders.computeIfAbsent(command.getOrderId(), id -> new Opened(
                command.getId(),
                pointIndex + networkLagPts,
                current.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli(),
                command)
        );
    }

    void cancelTrade(CancelOrderCommand cancel) {
        if (!cancel.getClientName().equals(clientName)) {
            return;
        }

        Opened remove = byIdOrders.remove(cancel.getOrderId());
        if (null != remove) {
            cancelled.put(
                    remove.getCommand().getOrderId(),
                    new Closed(
                    remove.getPairId(),
                    remove.getTimestamp(),
                    cancel.getCreatedTimestamp(),
                    remove.getCommand(),
                    null
            ));
        }
    }

    GetOrderResponse getTrade(GetOrderCommand order) {
        if (!order.getClientName().equals(clientName)) {
            return null;
        }

        Opened create = byIdOrders.get(order.getOrderId());
        if (null != create) {
            return new GetOrderResponse(
                    order.getClientName(),
                    order.getOrderId(),
                    orderDto(create, OrderStatus.NEW)
            );
        }

        Closed closed = done.get(order.getOrderId());

        if (null != closed) {
            return new GetOrderResponse(
                    order.getClientName(),
                    order.getOrderId(),
                    orderDto(closed, OrderStatus.FILLED)
            );
        }

        Closed cancel = cancelled.get(order.getOrderId());

        if (null != cancel) {
            return new GetOrderResponse(
                    order.getClientName(),
                    order.getOrderId(),
                    orderDto(cancel, OrderStatus.CANCELED)
            );
        }

        return null;
    }

    ListOpenOrdersResponse listOpen(ListOpenCommand list) {
        if (!list.getClientName().equals(clientName)) {
            return null;
        }

        return new ListOpenOrdersResponse(
                clientName,
                list.getId(),
                byIdOrders.values().stream().map(it -> orderDto(it, OrderStatus.NEW)).collect(Collectors.toList())
        );
    }

    private OrderDto orderDto(Opened opened, OrderStatus status) {
        return OrderDto.builder()
                .status(status)
                .orderId(opened.getCommand().getOrderId())
                .price(opened.getCommand().getPrice())
                .size(opened.getCommand().getAmount())
                .build();
    }

    private OrderDto orderDto(Closed closed, OrderStatus status) {
        return OrderDto.builder()
                .status(status)
                .orderId(closed.getCommand().getOrderId())
                .price(closed.getCommand().getPrice())
                .size(closed.getCommand().getAmount())
                .build();
    }

    void acceptOrderBook(OrderBook book) {
        if (!Double.isFinite(book.getBestSell())
                || !Double.isFinite(book.getBestBuy())
                || book.getBestSell() < EPSILON
                || book.getBestBuy() < EPSILON
                || book.getBestSell() > MAX_VAL
                || book.getBestBuy() > MAX_VAL) {
            return;
        }

        pointIndex++;
        computeMinMaxDate(book);
        Collection<Opened> open = byIdOrders.values();
        List<Opened> closed = open.stream()
                .filter(it -> it.getMinimalIndexThatCanClose() <= pointIndex)
                .filter(it -> canCompleteCommand(it.getCommand(), book))
                .collect(Collectors.toList());

        computeLockedBalance();
        computeTradeDeviations(book);

        if (closed.isEmpty()) {
            return;
        }

        closed.forEach(opn -> log.info("Satisfy(close) {} with {}", opn, book));
        open.removeAll(closed);
        closed.forEach(it ->
                done.put(
                        it.getCommand().getOrderId(),
                        new Closed(
                                it.getPairId(),
                                it.getTimestamp(),
                                book.getMeta().getTimestamp(),
                                it.getCommand(),
                                book)
                )
        );
    }

    void logStats() {
        long active = byIdOrders.size();

        log.info("In period {} to {} we have active {} / completed {} orders", min, max, active, done.size());

        reportJsonStats();
    }

    Map<String, BigDecimal> balances() {
        List<Opened> fromOrders = byIdOrders.values().stream()
                .filter(it -> it.getCommand().getAmount().compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.toList());
        List<Opened> toOrders = byIdOrders.values().stream()
                .filter(it -> it.getCommand().getAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        BigDecimal valueFrom = fromBal;

        for (Opened opened : fromOrders) {
            valueFrom = valueFrom.add(opened.getCommand().getAmount());
        }

        BigDecimal valueTo = toBal;
        for (Opened opened : toOrders) {
            valueTo = valueTo.subtract(opened.getCommand().getAmount().multiply(opened.getCommand().getPrice()));
        }

        return ImmutableMap.of(
                from.getCode(), valueFrom,
                to.getCode(), valueTo
        );
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private void reportJsonStats() {

        Map<TradingCurrency, BigDecimal> activeBalance = computeOrderBalance(asClosed(byIdOrders.values()));
        Map<TradingCurrency, BigDecimal> doneBalance = computeOrderBalance(done.values());
        Map<TradingCurrency, BigDecimal> cancelBalance = computeOrderBalance(cancelled.values());
        Map<TradingCurrency, BigDecimal> pairwiseDoneBalance = computeOrderBalance(computePaired(done.values()));
        Map<CurrencyPair, Map<Boolean, List<Double>>> pairwiseBestAmounts =
                computeClosingAmountsAtBest(computePaired(done.values()));
        Map<Boolean, List<Long>> timeToClose = computeTimeToClose(byIdOrders.values(), done.values(), MILLIS_IN_10M);

        log.info("--------------------------- Statistics for {} ------------------------", clientName);

        Supplier<Map<String, Object>> newMap = LinkedHashMap::new;
        Map<String, Object> report = newMap.get();
        Function<String, Map<String, Object>> reportKey = root ->
                (Map<String, Object>) report.computeIfAbsent(root, id -> new LinkedHashMap<String, Object>());

        report.put("active", byIdOrders.size());
        report.put("done", done.size());
        report.put("client", clientName);
        report.put("gain", envContainer.getFutureGainPct());
        report.put("threshold", envContainer.getNoopThreshold());
        report.put("chargeRatePct", envContainer.getChargeRatePct());
        report.put("start", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(min));
        report.put("end", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(max));
        activeBalance.forEach((k, v) -> reportKey.apply("activeBalance").put(k.getCode(), v));
        doneBalance.forEach((k, v) -> reportKey.apply("totalDone").put(k.getCode(), v));
        lockedBalance.forEach((k, v) -> reportKey.apply("lockMaxOnStat").put(k.getCode(), v));
        cancelBalance.forEach((k, v) -> reportKey.apply("cancelBalanceChange").put(k.getCode(), v));
        pairwiseDoneBalance.forEach((k, v) -> reportKey.apply("balanceChangeByDone").put(k.getCode(), v));
        pairwiseBestAmounts.forEach((pair, vals) -> vals.forEach((k, v) ->
            logSeriesStats(v, reportKey.apply("whenClosingBest" + (k ? "Sell" : "Buy") + "Amount"))
        ));
        timeToClose.forEach((k, v) ->
                logSeriesStats(v, reportKey.apply("timeToCloseWthreshold" + (k ? "Sell" : "Buy")))
        );
        logSeriesStats(
                timeToClose.values().stream().flatMap(Collection::stream).collect(Collectors.toList()),
                reportKey.apply("totalTimeToCloseWthreshold")
        );
        computeDoneDeviations().forEach((k, v) ->
                logSeriesStats(v, reportKey.apply("priceDeviationsPct" + (k ? "Sell" : "Buy")))
        );

        log.info("Porcelain `{}`", MAPPER.writer().writeValueAsString(report));
        log.info("{}", MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }

    private List<Closed> asClosed(Collection<Opened> openeds) {
        return openeds.stream().map(it ->
                new Closed(
                        it.getPairId(),
                        it.getTimestamp(),
                        it.getCommand().getCreatedTimestamp(),
                        it.getCommand(),
                        null
                )).collect(Collectors.toList());
    }

    private Map<Boolean, List<Double>> computeDoneDeviations() {
        Map<Boolean, List<Double>> doneDeviations = new HashMap<>();
        for (Closed closed : done.values()) {
            Double val = byIsSellByTradeIdDeviation
                    .getOrDefault(isSell(closed.getCommand()), Collections.emptyMap())
                    .get(closed.getPairId());
            if (null != val) {
                doneDeviations
                        .computeIfAbsent(isSell(closed.getCommand()), id -> new ArrayList<>())
                        .add(val);
            }
        }
        return doneDeviations;
    }

    private void computeTradeDeviations(OrderBook book) {
        Collection<Opened> opened = byIdOrders.values();

        for (Opened open : opened) {
            boolean isSell = isSell(open.getCommand());
            double deviationPrice = isSell ? book.getBestBuy() : book.getBestSell();
            double deviation = Math.abs(open.getCommand().getPrice().doubleValue() / deviationPrice * 100.0 - 100.0);

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

    private List<Closed> computePaired(Collection<Closed> closed) {
        Map<String, Long> pairOccurency = closed.stream().map(Closed::getPairId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return closed.stream()
                .filter(it -> pairOccurency.get(it.getPairId()) == 2)
                .collect(Collectors.toList());
    }

    private Map<Boolean, List<Long>> computeTimeToClose(
            Collection<Opened> open, Collection<Closed> closed, long threshold) {
        Map<Boolean, List<Long>> timeToClose = new HashMap<>();

        for (Opened val : open) {
            long dt = currentTimestamp - val.getTimestamp();
            if (dt < threshold) {
                continue;
            }

            timeToClose.computeIfAbsent(isSell(val.getCommand()), id -> new ArrayList<>()).add(dt);
        }

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

    private Map<TradingCurrency, BigDecimal> computeOrderBalance(Collection<Closed> closed) {
        Map<TradingCurrency, BigDecimal> doneBalance = new EnumMap<>(TradingCurrency.class);
        for (Closed val : closed) {
            BigDecimal from;
            BigDecimal to;

            if (isSell(val.getCommand())) {
                from = val.getCommand().getAmount();
                to = val.getCommand().getAmount().abs().multiply(val.getCommand().getPrice())
                        .multiply(getCharge());
            } else {
                from = val.getCommand().getAmount().multiply(getCharge());
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

    private void computeLockedBalance() {
        Map<TradingCurrency, BigDecimal> currentLocked = new EnumMap<>(TradingCurrency.class);
        Collection<Opened> open = byIdOrders.values();

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
        if (values.isEmpty()) {
            return;
        }

        DescriptiveStatistics statistics = new DescriptiveStatistics();
        values.forEach(it -> statistics.addValue(it.doubleValue()));
        target.put("count", values.size());
        target.put("mean", statistics.getMean());
        target.put("stdev", statistics.getStandardDeviation());
        target.put("percentile5", statistics.getPercentile(5.0));
        target.put("percentile10", statistics.getPercentile(10.0));
        target.put("percentile25", statistics.getPercentile(25.0));
        target.put("percentile50", statistics.getPercentile(50.0));
        target.put("percentile75", statistics.getPercentile(75.0));
        target.put("percentile90", statistics.getPercentile(90.0));
        target.put("percentile95", statistics.getPercentile(95.0));
    }

    private BigDecimal getCharge() {
        return config.getTradeChargeRatePct().movePointLeft(2).negate().add(BigDecimal.ONE);
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
        currentTimestamp = book.getMeta().getTimestamp();
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
