package com.gtc.opportunity.trader.service.nnopportunity.util;

import com.gtc.model.provider.AggregatedOrder;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.service.dto.FlatOrderBook;
import lombok.experimental.UtilityClass;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@UtilityClass
public final class BookFlattener {

    public static FlatOrderBook simplify(OrderBook book) {
        return new FlatOrderBook(
                (float) book.getBestSell(),
                (float) book.getBestBuy(),
                (float) (book.getHistogramBuy()[0].getMaxPrice() - book.getHistogramBuy()[0].getMinPrice()),
                (float) (book.getHistogramSell()[0].getMaxPrice() - book.getHistogramSell()[0].getMinPrice()),
                toAmountArray(book.getHistogramBuy()),
                toAmountArray(book.getHistogramSell())
        );
    }

    private static float[] toAmountArray(AggregatedOrder[] orders) {
        float[] result = new float[orders.length];
        for (int i = 0; i < orders.length; ++i) {
            result[i] = (float) orders[i].getAmount();
        }

        return result;
    }
}
