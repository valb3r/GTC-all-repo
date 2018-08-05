package com.gtc.opportunity.trader.service.stat;

import com.google.common.collect.ImmutableSet;
import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.CryptoPricing;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.CryptoPricingRepository;
import com.gtc.opportunity.trader.service.opportunity.creation.ConfigCache;
import com.newrelic.api.agent.NewRelic;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 04.08.18.
 */
@Service
@RequiredArgsConstructor
public class TradePerformanceCalculator {

    private static final BigDecimal DEFAULT_CHARGE_PCT = new BigDecimal("0.2");

    private static final String PATH = "<Path>";
    private static final String CAN_PROFIT_MILLI_BTC = "Custom/<Path>/Open/ExpectedProfitMilliBtc";
    private static final String ERR_LOST_MILLI_BTC = "Custom/<Path>/Error/LossMilliBtc";
    private static final String TOTAL_MILLI_BTC = "Custom/<Path>/Total/AmountMilliBtc";
    private static final String LATEST_TIME_TO_CLOSE = "Custom/<Path>/LatestTimeToCloseS";
    private static final String AMOUNT_IN_ORDERS_MILLI_BTC = "Custom/<Path>/Amount/LockedByOrdersMilliBTC";

    private static final Set<TradeStatus> ERRORS = ImmutableSet.of(
            TradeStatus.NEED_RETRY, TradeStatus.CANCELLED, TradeStatus.ERR_OPEN, TradeStatus.GEN_ERR);

    private static final Set<TradeStatus> OPEN = ImmutableSet.of(
            TradeStatus.UNKNOWN, TradeStatus.OPENED);

    private static final Set<TradeStatus> DONE = ImmutableSet.of(
            TradeStatus.CLOSED, TradeStatus.DONE_MAN);

    private final CryptoPricingRepository pricingRepository;
    private final ConfigCache configCache;

    @Transactional(readOnly = true)
    public <T> Performance calculateOnGroupedByPair(List<Trade> scopedTrades, Function<Trade, T> key) {
        Map<TradingCurrency, CryptoPricing> priceList = pricingRepository.priceList();
        BigDecimal expectedProfitBtc = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal inOrders = BigDecimal.ZERO;
        BigDecimal inErrors = BigDecimal.ZERO;

        Map<T, List<Trade>> grouped = scopedTrades.stream().collect(Collectors.groupingBy(key));

        for (List<Trade> trades : grouped.values()) {

            expectedProfitBtc = expectedProfitBtc.add(computeExpectedProfit(trades, priceList));

            inOrders = inOrders.add(
                    computeAmount(
                            trades.stream().filter(it -> OPEN.contains(it.getStatus())).collect(Collectors.toList()),
                            priceList)
            );

            inErrors = inErrors.add(
                    computeAmount(
                            trades.stream().filter(it -> ERRORS.contains(it.getStatus())).collect(Collectors.toList()),
                            priceList)
            );

            total = total.add(computeAmount(trades, priceList));
        }

        return new Performance(expectedProfitBtc, total, inOrders, inErrors, computeLatestTimeToClose(scopedTrades));
    }

    public void reportPerformance(String pathPrefix, Performance performance) {

        NewRelic.recordMetric(CAN_PROFIT_MILLI_BTC.replace(PATH, pathPrefix),
                performance.getExpectedProfitBtc().floatValue() * 1000.0f);
        NewRelic.recordMetric(TOTAL_MILLI_BTC.replace(PATH, pathPrefix),
                performance.getTotal().floatValue() * 1000.0f);
        NewRelic.recordMetric(AMOUNT_IN_ORDERS_MILLI_BTC.replace(PATH, pathPrefix),
                performance.getInOrders().floatValue() * 1000.0f);
        NewRelic.recordMetric(ERR_LOST_MILLI_BTC.replace(PATH, pathPrefix),
                performance.getInErrors().floatValue() * 1000.0f);
        NewRelic.recordMetric(LATEST_TIME_TO_CLOSE.replace(PATH, pathPrefix), performance.getLatestTimeToCloseS());
    }

    private BigDecimal computeExpectedProfit(List<Trade> trades, Map<TradingCurrency, CryptoPricing> priceList) {
        BigDecimal total = BigDecimal.ZERO;

        for (Trade trade : trades) {
            BigDecimal charge = configCache.getClientCfg(
                    trade.getClient().getName(),
                    trade.getCurrencyFrom(),
                    trade.getCurrencyTo())
                    .map(ClientConfig::getTradeChargeRatePct)
                    .orElse(DEFAULT_CHARGE_PCT).movePointLeft(2).negate().add(BigDecimal.ONE);

            CryptoPricing from = priceList.get(trade.getCurrencyFrom());
            CryptoPricing to = priceList.get(trade.getCurrencyTo());
            if (null == from || null == to) {
                continue;
            }

            if (trade.isSell()) {
                total = total.subtract(trade.getOpeningAmount().abs().multiply(from.getPriceBtc()));
                total = total.add(trade.getOpeningAmount().abs()
                        .multiply(trade.getOpeningPrice()).multiply(to.getPriceBtc()).multiply(charge));
            } else {
                total = total.add(trade.getOpeningAmount().abs().multiply(from.getPriceBtc()).multiply(charge));
                total = total.subtract(trade.getOpeningAmount().abs()
                        .multiply(trade.getOpeningPrice()).multiply(to.getPriceBtc()));
            }
        }

        return total;
    }

    private BigDecimal computeAmount(List<Trade> trades, Map<TradingCurrency, CryptoPricing> priceList) {
        BigDecimal total = BigDecimal.ZERO;

        for (Trade trade : trades) {
            CryptoPricing from = priceList.get(trade.getCurrencyFrom());
            if (null == from) {
                continue;
            }

            total = total.add(trade.getOpeningAmount().abs().multiply(from.getPriceBtc()));
        }
        return total;
    }

    private long computeLatestTimeToClose(List<Trade> trades) {
        Trade last = trades.stream()
                .filter(it -> DONE.contains(it.getStatus()))
                .max(Comparator.comparing(Trade::getStatusUpdated))
                .orElse(null);

        if (last == null) {
            return -1;
        }

        return ChronoUnit.SECONDS.between(last.getRecordedOn(), last.getStatusUpdated());
    }

    @Data
    public static class Performance {

        private final BigDecimal expectedProfitBtc;
        private final BigDecimal total;
        private final BigDecimal inOrders;
        private final BigDecimal inErrors;

        private final long latestTimeToCloseS;
    }
}
