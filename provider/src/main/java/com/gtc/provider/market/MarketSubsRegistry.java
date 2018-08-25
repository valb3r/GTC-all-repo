package com.gtc.provider.market;

import com.gtc.meta.TradingCurrency;
import com.gtc.model.provider.MarketPrice;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 25.08.18.
 */
@Component
@RequiredArgsConstructor
public class MarketSubsRegistry {

    private final Map<Key, Set<WebSocketSession>> marketPricesToSessionId = new ConcurrentHashMap<>();

    public void subscribeMarket(WebSocketSession session, TradingCurrency from, TradingCurrency to) {
        marketPricesToSessionId.computeIfAbsent(new Key(from, to), id -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    public Set<WebSocketSession> destinations(MarketPrice price) {
        return marketPricesToSessionId.getOrDefault(
                new Key(price.getFrom(), price.getTo()),
                Collections.emptySet()
        );
    }

    public Set<WebSocketSession> allSessions() {
        return marketPricesToSessionId.entrySet().stream()
                .flatMap(it -> it.getValue().stream())
                .collect(Collectors.toSet());
    }

    Map<TradingCurrency, Set<TradingCurrency>> dataToPoll() {
        return marketPricesToSessionId.keySet().stream().collect(
                Collectors.groupingBy(
                        Key::getFrom,
                        Collectors.mapping(
                                Key::getTo,
                                Collectors.toSet())
                )
        );
    }

    @Data
    private static class Key {

        private final TradingCurrency from;
        private final TradingCurrency to;
    }
}
