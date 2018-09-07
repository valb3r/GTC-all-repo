package com.gtc.opportunity.trader.service.nnopportunity.global;

import com.gtc.meta.CurrencyPair;
import com.gtc.model.provider.AggregatedOrder;
import com.gtc.model.provider.ByClientAndCurrency;
import com.gtc.model.provider.OrderBook;
import lombok.experimental.UtilityClass;
import org.apache.commons.csv.CSVRecord;

/**
 * Created by Valentyn Berezin on 31.07.18.
 */
@UtilityClass
public final class CsvRowToOrderBookMapper {

    public static OrderBook mapCsvToBook(String client, CurrencyPair pair, CSVRecord csv) {
        AggregatedOrder[] sell = new AggregatedOrder[10];
        AggregatedOrder[] buy = new AggregatedOrder[10];

        double bestBuy = dbl("Best buy", csv);
        double bestSell = dbl("Best sell", csv);

        double buyStep;
        double sellStep;

        // BUGFIX - old version used mixed up steps
        if (csv.toMap().containsKey("Histogram price buy step")) {
            buyStep = dbl("Histogram price sell step", csv); // mixed up
            sellStep = dbl("Histogram price buy step", csv);
        } else if (csv.toMap().containsKey("Histogram price Buy step")){
            buyStep = dbl("Histogram price Buy step", csv);
            sellStep = dbl("Histogram price Sell step", csv);
        } else {
            throw new IllegalStateException("Unknown version");
        }

        for (int i = 0; i < 10; ++i) {
            buy[i] = new AggregatedOrder(
                    bestBuy - buyStep * (10 - i),
                    bestBuy - buyStep * (9 - i),
                    dbl("Buy amount at " + i, csv),
                    0,
                    false,
                    (short) -(10 - i),
                    0, 0, 0);

            sell[i] = new AggregatedOrder(
                    bestSell + sellStep * i,
                    bestSell + sellStep * (i + 1),
                    dbl("Sell amount at " + i, csv),
                    0,
                    false,
                    (short) (i + 1),
                    0, 0, 0);
        }

        return OrderBook.builder()
                .meta(new ByClientAndCurrency(client, pair, Long.valueOf(csv.get("Time"))))
                .bestBuy(bestBuy)
                .bestSell(bestSell)
                .histogramBuy(buy)
                .histogramSell(sell)
                .build();
    }

    private static double dbl(String key, CSVRecord record) {
        return Double.valueOf(record.get(key));
    }
}

