package com.gtc.persistor.service;

import com.gtc.model.provider.OrderBook;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.gtc.persistor.config.Const.Persist.PERSIST_S;

/**
 * Created by Valentyn Berezin on 01.07.18.
 */
@Service
@RequiredArgsConstructor
public class BookPersistor {

    private final OrderBookRepository bookRepository;

    @Scheduled(fixedDelayString = PERSIST_S)
    public void persist() {
        List<OrderBook> orderBooks = new ArrayList<>(bookRepository.getOrders());
        bookRepository.clear();
    }
}
