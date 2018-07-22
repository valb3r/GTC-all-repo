package com.gtc.persistor.service;

import com.gtc.model.provider.OrderBook;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Valentyn Berezin on 01.07.18.
 */
@Component
public class OrderBookRepository {

    private final List<OrderBook> orders = new CopyOnWriteArrayList<>();

    void storeOrderBook(OrderBook book) {
        orders.add(book);
    }

    List<OrderBook> getOrders() {
        return orders;
    }

    void clear() {
        orders.clear();
    }
}
