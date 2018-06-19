package com.gtc.opportunity.trader.cqe.domain;

import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.gtc.meta.CurrencyPair;
import com.gtc.model.provider.OrderBook;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;

import java.util.concurrent.atomic.AtomicLong;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

/**
 * Created by Valentyn Berezin on 16.06.18.
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "version"})
public class IndexedOrderBook {

    private static final AtomicLong VERSION_GENERATOR = new AtomicLong();

    public static final SimpleAttribute<IndexedOrderBook, String> A_ID = attribute("id", IndexedOrderBook::getId);
    public static final SimpleAttribute<IndexedOrderBook, String> CLIENT = attribute(
            "client", o -> o.getMeta().getClient()
    );

    public static final SimpleAttribute<IndexedOrderBook, CurrencyPair> CURRENCY_PAIR = attribute(
            "currencyPair", o -> o.getMeta().getPair()
    );

    public static final SimpleAttribute<IndexedOrderBook, Double> BEST_BUY = attribute(
            "bestBuy", IndexedOrderBook::getBestBuy
    );

    public static final SimpleAttribute<IndexedOrderBook, Double> BEST_SELL = attribute(
            "bestSell", IndexedOrderBook::getBestSell
    );

    private final long version = VERSION_GENERATOR.getAndIncrement();

    private final String id;

    @Delegate
    private final OrderBook book;

    public static String calculateId(OrderBook orderBook) {
        return orderBook.getMeta().getClient()
                + orderBook.getMeta().getPair().getFrom()
                + orderBook.getMeta().getPair().getTo();
    }
}
