package com.gtc.opportunity.trader.service;

import com.gtc.meta.TradingCurrency;
import com.gtc.model.provider.OrderBook;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Valentyn Berezin on 03.09.18.
 */
@Service
public class LatestMarketPrices {

    private final Map<String, BigDecimal> latestBestSell = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> latestBestBuy = new ConcurrentHashMap<>();

    public void addPrice(OrderBook book) {
        String key = key(book);
        latestBestSell.put(key, BigDecimal.valueOf(book.getBestSell()));
        latestBestBuy.put(key, BigDecimal.valueOf(book.getBestBuy()));
    }

    public BigDecimal bestSell(String client, TradingCurrency from, TradingCurrency to) {
        return latestBestSell.get(key(client, from, to));
    }

    public BigDecimal bestBuy(String client, TradingCurrency from, TradingCurrency to) {
        return latestBestBuy.get(key(client, from, to));
    }

    private static String key(OrderBook book) {
        return key(book.getMeta().getClient(), book.getMeta().getPair().getFrom(), book.getMeta().getPair().getTo());
    }

    private static String key(String client, TradingCurrency from, TradingCurrency to) {
        return client + from.getCode() + to.getCode();
    }
}
