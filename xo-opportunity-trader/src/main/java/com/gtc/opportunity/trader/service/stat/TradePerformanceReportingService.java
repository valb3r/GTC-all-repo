package com.gtc.opportunity.trader.service.stat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.*;
import com.gtc.opportunity.trader.repository.AcceptedXoTradeRepository;
import com.gtc.opportunity.trader.repository.CryptoPricingRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.repository.WalletRepository;
import com.gtc.opportunity.trader.repository.stat.rejected.XoTradeRejectedStatTotalRepository;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 25.06.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradePerformanceReportingService {

    private static final String AMOUNT_RAW = "Custom/Amount/RAW";
    private static final String AMOUNT_USD = "Custom/Amount/USD";
    private static final String AMOUNT_MILLI_BTC = "Custom/Amount/MilliBTC";
    private static final String AMOUNT_IN_ORDERS_MILLI_BTC = "Custom/Amount/InOrdersMilliBTC";

    private static final String REJECTED_ALL_COUNT = "Custom/Rejected/All";
    private static final String REJECTED_ALL_WITH_ENABL_CONFIG_COUNT = "Custom/Rejected/AllEnabledConfigured";
    private static final String REJECTED_LOW_BAL_COUNT = "Custom/Rejected/LowBalance";
    private static final String REJECTED_GEN_ERR = "Custom/Rejected/GenError";
    private static final String REJECTED_OPT_FAIL = "Custom/Rejected/OptFail";
    private static final String REJECTED_SINGLE_SIDE_LIMIT = "Custom/Rejected/SingleSideLimit";

    private static final String ACCEPTED_UNKNOWN = "Custom/Accepted/Unknown";
    private static final String ACCEPTED_OPEN = "Custom/Accepted/Open";
    private static final String ACCEPTED_CLOSED = "Custom/Accepted/Closed";
    private static final String ACCEPTED_OTHER = "Custom/Accepted/Other";

    private static final String ACCEPTED_XO_CAN_PROFIT_MILLI_BTC = "Custom/Accepted/Xo/Open/ExpectedProfitMilliBtc";
    private static final String ACCEPTED_XO_PROFIT_MILLI_BTC = "Custom/Accepted/Xo/Closed/ProfitMilliBtc";
    private static final String ACCEPTED_ERR_LOST_MILLI_BTC = "Custom/Accepted/Xo/Error/LossMilliBtc";
    private static final String ACCEPTED_TOTAL_MILLI_BTC = "Custom/Accepted/Xo/Total/AmountMilliBtc";
    private static final String LATEST_TIME_TO_CLOSE = "Custom/Accepted/Xo/LatestTimeToCloseS";

    private static final Set<TradeStatus> ERRORS = ImmutableSet.of(
            TradeStatus.NEED_RETRY, TradeStatus.CANCELLED, TradeStatus.ERR_OPEN, TradeStatus.GEN_ERR);

    private static final Set<TradeStatus> OPEN = ImmutableSet.of(
            TradeStatus.UNKNOWN, TradeStatus.OPENED);

    private static final Set<TradeStatus> DONE = ImmutableSet.of(
            TradeStatus.CLOSED, TradeStatus.DONE_MAN);

    private final XoTradeRejectedStatTotalRepository rejectedStatRepository;
    private final AcceptedXoTradeRepository xoTradeRepository;
    private final TradeRepository tradeRepository;
    private final WalletRepository walletRepository;
    private final CryptoPricingRepository pricingRepository;

    @Trace(dispatcher = true)
    @Transactional(readOnly = true)
    @Scheduled(fixedRateString = "#{${app.schedule.reportTradePerformanceS} * 1000}")
    public void reportPerformance() {
        long all = rejectedStatRepository.rejectedCount();
        NewRelic.recordMetric(REJECTED_ALL_COUNT, all);
        NewRelic.recordMetric(REJECTED_LOW_BAL_COUNT, rejectedStatRepository
                .rejectedCountByLikeReason(Reason.LOW_BAL.getMsg()));
        NewRelic.recordMetric(REJECTED_GEN_ERR, rejectedStatRepository
                .rejectedCountByLikeReason(Reason.GEN_ERR.getMsg()));
        NewRelic.recordMetric(REJECTED_OPT_FAIL, rejectedStatRepository
                .rejectedCountByLikeReason(Reason.OPT_CONSTR_FAIL.getMsg()));
        NewRelic.recordMetric(REJECTED_ALL_WITH_ENABL_CONFIG_COUNT,
                all
                        - rejectedStatRepository.rejectedCountByLikeReason(Reason.NO_CONFIG.getMsg())
                        - rejectedStatRepository.rejectedCountByLikeReason(Reason.DISABLED.getMsg())
        );
        NewRelic.recordMetric(REJECTED_SINGLE_SIDE_LIMIT, rejectedStatRepository
                .rejectedCountByLikeReason(Reason.SIDE_LIMIT.getMsg()));

        NewRelic.recordMetric(ACCEPTED_UNKNOWN, tradeRepository.countAllByStatusEquals(TradeStatus.UNKNOWN));
        NewRelic.recordMetric(ACCEPTED_OPEN, tradeRepository.countAllByStatusEquals(TradeStatus.OPENED));
        NewRelic.recordMetric(ACCEPTED_CLOSED,
                tradeRepository.countAllByStatusEquals(TradeStatus.CLOSED)
                        + tradeRepository.countAllByStatusEquals(TradeStatus.DONE_MAN));
        NewRelic.recordMetric(ACCEPTED_OTHER, tradeRepository.countAllByStatusNotIn(
                ImmutableSet.of(TradeStatus.UNKNOWN, TradeStatus.OPENED, TradeStatus.CLOSED)
        ));

        Map<TradingCurrency, CryptoPricing> priceList = new EnumMap<>(TradingCurrency.class);
        pricingRepository.findAll().forEach(w -> priceList.put(w.getCurrency(), w));

        reportWalletValue(priceList);
        reportXoPerformanceValue(priceList);
        reportLatestTimeToClose();
    }

    private void reportWalletValue(Map<TradingCurrency, CryptoPricing> priceList) {
        BigDecimal rawValue = BigDecimal.ZERO;
        BigDecimal usdValue = BigDecimal.ZERO;
        BigDecimal btcValue = BigDecimal.ZERO;

        for (Wallet wallet : walletRepository.findByBalanceGreaterThan(BigDecimal.ZERO)) {
            rawValue = rawValue.add(wallet.getBalance());
            CryptoPricing pricing = priceList.get(wallet.getCurrency());
            if (null == pricing) {
                continue;
            }

            usdValue = usdValue.add(wallet.getBalance().multiply(pricing.getPriceUsd()));
            btcValue = btcValue.add(wallet.getBalance().multiply(pricing.getPriceBtc()));
        }

        NewRelic.recordMetric(AMOUNT_RAW, rawValue.floatValue());
        NewRelic.recordMetric(AMOUNT_USD, usdValue.floatValue());
        NewRelic.recordMetric(AMOUNT_MILLI_BTC, btcValue.floatValue() * 1000.0f);
    }

    private void reportXoPerformanceValue(Map<TradingCurrency, CryptoPricing> priceList) {
        BigDecimal expectedProfitBtc = BigDecimal.ZERO;
        BigDecimal profitBtc = BigDecimal.ZERO;
        BigDecimal errorLossBtc = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal inOrders = BigDecimal.ZERO;

        // Awkward solution, but it is more robust since higher level machine is not updated
        for (AcceptedXoTrade xoTrade : xoTradeRepository.findAll()) {
            CryptoPricing price = priceList.get(xoTrade.getCurrencyFrom());
            if (null == price) {
                continue;
            }

            Collection<Trade> trades = tradeRepository.findByXoOrderId(xoTrade.getId());
            List<TradeStatus> statuses = trades.stream().map(Trade::getStatus).collect(Collectors.toList());
            long errorCount = errorCount(statuses);

            // loss of all items is not as severe as partial loss - it is basically NOP
            if (errorCount > 0 && errorCount < statuses.size()) {
                errorLossBtc = getInErrors(errorLossBtc, price, trades);
            } else if (isOpen(statuses)) {
                expectedProfitBtc = expectedProfitBtc.add(xoTrade.getExpectedProfit().multiply(price.getPriceBtc()));
                inOrders = extractInOrders(inOrders, price, trades);
            } else if (isDone(statuses)) {
                profitBtc = profitBtc.add(xoTrade.getExpectedProfit().multiply(price.getPriceBtc()));
            }
            total = total.add(xoTrade.getAmount().multiply(price.getPriceBtc()));
        }

        NewRelic.recordMetric(ACCEPTED_XO_CAN_PROFIT_MILLI_BTC, expectedProfitBtc.floatValue() * 1000.0f);
        NewRelic.recordMetric(ACCEPTED_XO_PROFIT_MILLI_BTC, profitBtc.floatValue() * 1000.0f);
        NewRelic.recordMetric(ACCEPTED_ERR_LOST_MILLI_BTC, errorLossBtc.floatValue() * 1000.0f);
        NewRelic.recordMetric(ACCEPTED_TOTAL_MILLI_BTC, total.floatValue() * 1000.0f);
        NewRelic.recordMetric(AMOUNT_IN_ORDERS_MILLI_BTC, inOrders.floatValue() * 1000.0f);
    }

    private void reportLatestTimeToClose() {
        Trade last = Iterables.getFirst(tradeRepository.findLatestByStatus(TradeStatus.CLOSED), null);
        if (null == last) {
            return;
        }

        NewRelic.recordMetric(
                LATEST_TIME_TO_CLOSE,
                ChronoUnit.SECONDS.between(last.getRecordedOn(), last.getStatusUpdated())
        );
    }

    private BigDecimal extractInOrders(BigDecimal inOrders, CryptoPricing price, Collection<Trade> trades) {
        for (Trade trade : trades) {
            if (!isOpen(Collections.singletonList(trade.getStatus()))) {
                continue;
            }

            inOrders = inOrders.add(trade.getAmount().abs().multiply(price.getPriceBtc()));
        }
        return inOrders;
    }

    private BigDecimal getInErrors(BigDecimal errorLossBtc, CryptoPricing price, Collection<Trade> trades) {
        Collection<Trade> withIssue = trades.stream()
                .filter(it -> errorCount(Collections.singletonList(it.getStatus())) > 0)
                .collect(Collectors.toSet());
        for (Trade trade : withIssue) {
            if (errorCount(Collections.singletonList(trade.getStatus())) == 0) {
                continue;
            }
            errorLossBtc = errorLossBtc.add(trade.getAmount().abs().multiply(price.getPriceBtc()));
        }
        return errorLossBtc;
    }

    private long errorCount(List<TradeStatus> statuses) {
        return statuses.stream().filter(ERRORS::contains).count();
    }

    private boolean isOpen(List<TradeStatus> statuses) {
        return statuses.stream().anyMatch(OPEN::contains);
    }

    private boolean isDone(List<TradeStatus> statuses) {
        // IDEA-suggested replacement is incorrect
        return statuses.stream().allMatch(DONE::contains);
    }
}
