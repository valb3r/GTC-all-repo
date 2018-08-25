package com.gtc.provider.service;

import com.gtc.meta.CurrencyPair;
import com.gtc.model.provider.*;
import com.gtc.provider.clients.MarketDto;
import com.gtc.provider.clients.WsClient;
import com.gtc.provider.config.WriteConf;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static com.gtc.provider.config.Const.CONF_ROOT_SCHEDULE_CHILD;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsReporter {

    private static final String MESSAGE = "Custom/DbStats/";

    private static final String SPEL_HISTO_RATE = "${" + CONF_ROOT_SCHEDULE_CHILD + "stats.histogramMS}";
    private static final String SPEL_TICKER_RATE = "${" + CONF_ROOT_SCHEDULE_CHILD + "stats.tickerMS}";
    private static final String SPEL_DELAY_INI = "#{${" + CONF_ROOT_SCHEDULE_CHILD + "stats.initialDelayS} * 1000}";

    private final HistogramBuilder histogramBuilder;
    private final DeclaredClientsProvider clients;
    private final SubsRegistry subsRegistry;
    private final WriteConf writeConf;

    @Value(SPEL_HISTO_RATE)
    private int histoDelay;

    @Value(SPEL_TICKER_RATE)
    private int tickerDelay;

    @Value(SPEL_DELAY_INI)
    private int initialDelay;

    @Trace(dispatcher = true)
    @Scheduled(fixedRateString = SPEL_HISTO_RATE, initialDelayString = SPEL_DELAY_INI)
    public void writeHistogramStats() {
        try {
            NewRelic.incrementCounter(MESSAGE + "histogram", 1);
            getWriteableClients()
                    .forEach(client -> writeHistogramIfNeeded(client.name(), client.market()));
        } catch (RuntimeException ex) {
            NewRelic.noticeError(ex);
            log.error("Failed writing histogram stats", ex);
        }
    }

    @Trace(dispatcher = true)
    @Scheduled(fixedRateString = SPEL_TICKER_RATE, initialDelayString = SPEL_DELAY_INI)
    public void writeTickerStats() {
        try {
            NewRelic.incrementCounter(MESSAGE + "ticker", 1);
            getWriteableClients()
                    .forEach(client -> writeTickerIfNeeded(client.name(), client.market()));
        } catch (RuntimeException ex) {
            NewRelic.noticeError(ex);
            log.error("Failed writing ticker stats", ex);
        }
    }

    private Stream<? extends WsClient> getWriteableClients() {
        return clients.getClientList().stream()
                .filter(client -> !client.isDisconnected())
                .filter(client -> System.currentTimeMillis() - client.connectedAtTimestamp() > initialDelay);
    }

    private void writeTickerIfNeeded(String clientName, MarketDto stats) {

        stats.getTicker().forEach((pair, value) ->
                subsRegistry.publishTicker(
                        Ticker.builder()
                                .meta(new ByClientAndCurrency(clientName, pair))
                                .tickerDate(value.getTimestamp())
                                .price(value.getValue())
                                .build())
        );
    }

    private void writeHistogramIfNeeded(String clientName, MarketDto stats) {
        stats.getMarket().forEach((currency, market) ->
                subsRegistry.publishOrderBook(writeMarketHistory(clientName, currency, market))
        );
    }

    private OrderBook writeMarketHistory(String clientName, CurrencyPair pair, Collection<Bid> market) {

        Stat stat = calcStat(market);
        OrderBook history = OrderBook.builder()
                .meta(new ByClientAndCurrency(clientName, pair))
                .bidCount(market.size())
                // best buy/sell - means market buys/sells
                .bestBuy(stat.getBestBuy())
                .bestSell(stat.getBestSell())
                .amountBestBuy(stat.getBestBuyAmount())
                .amountBestSell(stat.getBestSellAmount())
                .build();

        // separate histogram for buy (amount > 0) and sell
        buildHistogram(history, stat.getSell(), stat.getBuy());
        return history;
    }

    private void buildHistogram(OrderBook history, List<Bid> sellBids, List<Bid> buyBids) {
        double histogramSellLimit = history.getBestSell()
                * (1.0 + writeConf.getHistogram().getDeviateFromSignChangePct() / 100.0);
        double histogramBuyLimit = history.getBestBuy()
                * (1.0 - writeConf.getHistogram().getDeviateFromSignChangePct() / 100.0);

        history.setHistogramBuy(buildBuy(buyBids, histogramBuyLimit));
        history.setHistogramSell(buildSell(sellBids, histogramSellLimit));
    }

    private AggregatedOrder[] buildSell(List<Bid> sellBids, double histogramSellLimit) {
        List<Bid> bids = new ArrayList<>();
        for (Bid sellBid : sellBids) {
            if (sellBid.getPriceMin() <= histogramSellLimit) {
                bids.add(sellBid);
            }
        }
        return histogramBuilder.buildHistogram(bids, true);
    }

    private AggregatedOrder[] buildBuy(List<Bid> buyBids, double histogramBuyLimit) {
        List<Bid> bids = new ArrayList<>();
        for (Bid buyBid : buyBids) {
            if (buyBid.getPriceMax() >= histogramBuyLimit) {
                bids.add(buyBid);
            }
        }
        return histogramBuilder.buildHistogram(bids, false);
    }

    private Stat calcStat(Collection<Bid> market) {
        double bestBuy = Double.MIN_VALUE;
        double bestSell = Double.MAX_VALUE;
        double bestBuyAmount = 0.0;
        double bestSellAmount = 0.0;

        List<Bid> sell = new ArrayList<>(market.size());
        List<Bid> buy = new ArrayList<>(market.size());

        for (Bid bid : market) {

            if (bid.getAmount() > 0) {
                buy.add(bid);
                if (bestBuy < bid.getPriceMax()) {
                    bestBuy = bid.getPriceMax();
                    bestBuyAmount = bid.getAmount();
                }
            } else {
                sell.add(bid);
                if (bestSell > bid.getPriceMin()) {
                    bestSell = bid.getPriceMin();
                    bestSellAmount = bid.getAmount();
                }
            }
        }

        return new Stat(bestBuy, bestSell, Math.abs(bestBuyAmount), Math.abs(bestSellAmount), sell, buy);
    }

    @Getter
    @RequiredArgsConstructor
    private static class Stat {

        private final double bestBuy;
        private final double bestSell;
        private final double bestBuyAmount;
        private final double bestSellAmount;

        private final List<Bid> sell;
        private final List<Bid> buy;
    }
}
