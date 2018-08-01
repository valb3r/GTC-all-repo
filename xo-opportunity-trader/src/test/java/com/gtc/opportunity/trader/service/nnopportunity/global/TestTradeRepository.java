package com.gtc.opportunity.trader.service.nnopportunity.global;

import com.google.common.collect.ImmutableMap;
import com.gtc.meta.CurrencyPair;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.command.create.MultiOrderCreateCommand;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.domain.ClientConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 31.07.18.
 */
@Slf4j
@RequiredArgsConstructor
class TestTradeRepository {

    private final Map<String, Map<CurrencyPair, List<Opened>>> byClientPairOrders =
            new ConcurrentHashMap<>();

    private final List<Closed> done = new CopyOnWriteArrayList<>();

    private LocalDateTime min = LocalDateTime.MAX;
    private LocalDateTime max = LocalDateTime.MIN;
    private LocalDateTime current = LocalDateTime.MIN;

    private final Map<String, ClientConfig> configs;

    void acceptTrade(MultiOrderCreateCommand command) {
        Map<CurrencyPair, List<Opened>> orders =
                byClientPairOrders.computeIfAbsent(command.getClientName(), id -> new ConcurrentHashMap<>());

        command.getCommands().forEach(cmd -> {
            CurrencyPair pair = new CurrencyPair(
                    TradingCurrency.fromCode(cmd.getCurrencyFrom()),
                    TradingCurrency.fromCode(cmd.getCurrencyTo())
            );

            orders.computeIfAbsent(pair, id -> new CopyOnWriteArrayList<>())
                    .add(new Opened(current.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli(), cmd));
        });
    }

    void acceptOrderBook(OrderBook book) {
        computeMinMaxDate(book);
        List<Opened> open = byClientPairOrders
                .getOrDefault(book.getMeta().getClient(), ImmutableMap.of())
                .getOrDefault(book.getMeta().getPair(), Collections.emptyList());
        List<Opened> closed = open.stream()
                .filter(it -> canCompleteCommand(it.getCommand(), book))
                .collect(Collectors.toList());

        if (closed.isEmpty()) {
            return;
        }

        closed.forEach(opn -> log.info("Satisfy(close) {} with {}", opn, book));
        open.removeAll(closed);
        done.addAll(
                closed.stream().map(it -> new Closed(
                        it.getTimestamp(), book.getMeta().getTimestamp(), it.getCommand())
                ).collect(Collectors.toList())
        );
    }

    void logStats() {
        log.info("In period {} to {} opened {} / completed {} orders",
                min,
                max,
                byClientPairOrders.entrySet().stream()
                        .flatMap(it -> it.getValue().values().stream())
                        .mapToLong(Collection::size)
                        .sum(),
                done.size());

        configs.keySet().forEach(this::computeStatsForClient);
    }

    private void computeStatsForClient(String client) {
        Map<TradingCurrency, BigDecimal> doneBalance = new HashMap<>();
        Map<Boolean, List<Long>> timeToClose = new HashMap<>();

        for (Closed val : done) {
            BigDecimal from;
            BigDecimal to;

            boolean isSell = val.getCommand().getAmount().compareTo(BigDecimal.ZERO) < 0;

            if (isSell) {
                from = val.getCommand().getAmount();
                to = val.getCommand().getAmount().abs().multiply(val.getCommand().getPrice())
                        .multiply(getCharge(client));
            } else {
                from = val.getCommand().getAmount().multiply(getCharge(client));
                to = val.getCommand().getAmount().multiply(val.getCommand().getPrice()).negate();
            }

            doneBalance.compute(
                    TradingCurrency.fromCode(val.getCommand().getCurrencyFrom()),
                    (id, bal) -> null == bal ? BigDecimal.ZERO : bal.add(from)
            );
            doneBalance.compute(
                    TradingCurrency.fromCode(val.getCommand().getCurrencyTo()),
                    (id, bal) -> null == bal ? BigDecimal.ZERO : bal.add(to)
            );

            timeToClose.computeIfAbsent(isSell, id -> new ArrayList<>()).add(
                    val.getTimestampClose() - val.getTimestampOpen()
            );
        }

        log.info("--------------------------- Statistics for {} ------------------------", client);
        doneBalance.forEach((k, v) -> log.info("{} {}", k, v));
        log.info("Order closing time statistics");
        timeToClose.forEach((k, v) -> {
            log.info("{}:", k ? "SELL" : "BUY");
            logSeriesStats(v);
        });
    }

    private void logSeriesStats(List<Long> values) {
        DescriptiveStatistics statistics = new DescriptiveStatistics();
        values.forEach(statistics::addValue);
        log.info("Mean: {}", statistics.getMean());
        log.info("Stdev: {}", statistics.getStandardDeviation());
        log.info("25percentile: {}", statistics.getPercentile(25.0));
        log.info("50percentile: {}", statistics.getPercentile(50.0));
        log.info("75percentile: {}", statistics.getPercentile(75.0));
        log.info("90percentile: {}", statistics.getPercentile(90.0));
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

        private final long timestampOpen;
        private final long timestampClose;
        private final CreateOrderCommand command;
    }

    @Data
    private static class Opened {

        private final long timestamp;
        private final CreateOrderCommand command;
    }
}
