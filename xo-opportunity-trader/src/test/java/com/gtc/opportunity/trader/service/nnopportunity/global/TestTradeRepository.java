package com.gtc.opportunity.trader.service.nnopportunity.global;

import com.google.common.collect.ImmutableMap;
import com.gtc.meta.CurrencyPair;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.command.create.MultiOrderCreateCommand;
import com.gtc.model.provider.OrderBook;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 31.07.18.
 */
@Slf4j
class TestTradeRepository {

    private final Map<String, Map<CurrencyPair, List<CreateOrderCommand>>> byClientPairOrders =
            new ConcurrentHashMap<>();

    private final List<CreateOrderCommand> done = new CopyOnWriteArrayList<>();

    private LocalDateTime min = LocalDateTime.MAX;
    private LocalDateTime max = LocalDateTime.MIN;

    void acceptTrade(MultiOrderCreateCommand command) {
        Map<CurrencyPair, List<CreateOrderCommand>> orders =
                byClientPairOrders.computeIfAbsent(command.getClientName(), id -> new ConcurrentHashMap<>());

        command.getCommands().forEach(cmd -> {
            CurrencyPair pair = new CurrencyPair(
                    TradingCurrency.fromCode(cmd.getCurrencyFrom()),
                    TradingCurrency.fromCode(cmd.getCurrencyTo())
            );

            orders.computeIfAbsent(pair, id -> new CopyOnWriteArrayList<>()).add(cmd);
        });
    }

    void acceptOrderBook(OrderBook book) {
        computeMinMaxDate(book);
        List<CreateOrderCommand> open = byClientPairOrders
                .getOrDefault(book.getMeta().getClient(), ImmutableMap.of())
                .getOrDefault(book.getMeta().getPair(), Collections.emptyList());
        List<CreateOrderCommand> closed = open.stream()
                .filter(it -> canCompleteCommand(it, book))
                .collect(Collectors.toList());

        if (closed.isEmpty()) {
            return;
        }

        closed.forEach(opn -> log.info("Satisfy(close) {} with {}", opn, book));
        open.removeAll(closed);
        done.addAll(closed);
    }

    void logStats() {
    }

    private boolean canCompleteCommand(CreateOrderCommand command, OrderBook book) {
        if (command.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            return command.getPrice().compareTo(BigDecimal.valueOf(book.getBestBuy())) <= 0;
        } else {
            return command.getPrice().compareTo(BigDecimal.valueOf(book.getBestSell())) >= 0;
        }
    }

    private void computeMinMaxDate(OrderBook book) {
        LocalDateTime time = LocalDateTime.from(Instant.ofEpochMilli(book.getMeta().getTimestamp()));
        min = time.compareTo(min) < 0 ? time : min;
        max = time.compareTo(max) > 0 ? time : max;
    }
}
