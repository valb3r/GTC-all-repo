package com.gtc.provider.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.provider.MarketPrice;
import com.gtc.model.provider.OrderBook;
import com.gtc.model.provider.Ticker;
import com.gtc.provider.market.MarketSubsRegistry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 15.06.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubsRegistry {

    private final ObjectMapper mapper;
    private final MarketSubsRegistry marketSubsRegistry;

    private final Map<String, Set<WebSocketSession>> tickerClientToSessionId = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> bookClientToSessionId = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public void subscribeTicker(WebSocketSession session, String clientName) {
        tickerClientToSessionId.computeIfAbsent(clientName, id -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void subscribeBook(WebSocketSession session, String clientName) {
        bookClientToSessionId.computeIfAbsent(clientName, id -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void subscribeMarket(WebSocketSession session, TradingCurrency from, TradingCurrency to) {
        marketSubsRegistry.subscribeMarket(session, from, to);
    }

    public void unsubscribe(WebSocketSession session) {
        tickerClientToSessionId.forEach((k, v) -> v.remove(session));
        bookClientToSessionId.forEach((k, v) -> v.remove(session));
        locks.remove(session.getId());
    }

    @SneakyThrows
    public void publishTicker(Ticker ticker) {
        String msg = mapper.writeValueAsString(ticker);
        tickerClientToSessionId
                .getOrDefault(ticker.getMeta().getClient(), Collections.emptySet())
                .forEach(it -> doSend(it, msg));
    }

    @SneakyThrows
    public void publishOrderBook(OrderBook orderBook) {
        String msg = mapper.writeValueAsString(orderBook);
        bookClientToSessionId
                .getOrDefault(orderBook.getMeta().getClient(), Collections.emptySet())
                .forEach(it -> doSend(it, msg));
    }

    @SneakyThrows
    public void publishMarketPrice(MarketPrice price) {
        String msg = mapper.writeValueAsString(price);
        marketSubsRegistry.destinations(price).forEach(it -> doSend(it, msg));
    }

    @SneakyThrows
    @Scheduled(fixedRateString = "${app.schedule.pingMs}")
    public void ping() {
        Set<WebSocketSession> sessions = new HashSet<>();

        sessions.addAll(tickerClientToSessionId.entrySet().stream()
                .flatMap(it -> it.getValue().stream())
                .collect(Collectors.toSet()));
        sessions.addAll(bookClientToSessionId.entrySet().stream()
                .flatMap(it -> it.getValue().stream())
                .collect(Collectors.toSet()));

        sessions.addAll(marketSubsRegistry.allSessions());

        String msg = mapper.writeValueAsString(new Ping());
        sessions.forEach(subs -> doSend(subs, msg));
    }

    @SneakyThrows
    private void doSend(WebSocketSession session, String msg) {
        synchronized (locks.computeIfAbsent(session.getId(), id -> new Object())) {
            try {
                session.sendMessage(new TextMessage(msg));
            } catch (IllegalStateException ex) {
                unsubscribe(session);
            }
        }
    }

    @Data
    private static class Ping {

        private final long timestamp = System.currentTimeMillis();
    }
}
