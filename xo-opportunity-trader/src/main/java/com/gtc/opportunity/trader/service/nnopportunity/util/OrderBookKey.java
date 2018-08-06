package com.gtc.opportunity.trader.service.nnopportunity.util;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.service.nnopportunity.solver.Key;
import lombok.experimental.UtilityClass;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@UtilityClass
public final class OrderBookKey {

    public static Key key(OrderBook book) {
        return new Key(book.getMeta().getClient(), book.getMeta().getPair());
    }
}
