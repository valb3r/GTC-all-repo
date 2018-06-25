package com.gtc.opportunity.trader.service.stat;

import com.google.common.collect.ImmutableSet;
import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.*;
import com.gtc.opportunity.trader.repository.AcceptedXoTradeRepository;
import com.gtc.opportunity.trader.repository.CryptoPricingRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.repository.WalletRepository;
import com.gtc.opportunity.trader.repository.stat.rejected.XoTradeRejectedStatTotalRepository;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason;
import com.newrelic.api.agent.NewRelic;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Valentyn Berezin on 25.06.18.
 */
@Service
@RequiredArgsConstructor
public class TradePerformanceReportingService {

    private static final String AMOUNT_RAW = "Custom/Amount/RAW";
    private static final String AMOUNT_USD = "Custom/Amount/USD";
    private static final String AMOUNT_BTC = "Custom/Amount/BTC";

    private static final String REJECTED_ALL_COUNT = "Custom/Rejected/All";
    private static final String REJECTED_ALL_WITH_ENABL_CONFIG_COUNT = "Custom/Rejected/AllEnabledConfigured";
    private static final String REJECTED_LOW_BAL_COUNT = "Custom/Rejected/LowBalance";
    private static final String REJECTED_GEN_ERR = "Custom/Rejected/GenError";
    private static final String REJECTED_OPT_FAIL = "Custom/Rejected/OptFail";

    private static final String ACCEPTED_UNKNOWN = "Custom/Accepted/Unknown";
    private static final String ACCEPTED_OPEN = "Custom/Accepted/Open";
    private static final String ACCEPTED_CLOSED = "Custom/Accepted/Closed";
    private static final String ACCEPTED_OTHER = "Custom/Accepted/Other";

    private static final String ACCEPTED_XO_CAN_PROFIT_BTC = "Custom/Accepted/Xo/Open/ExpectedProfitBtc";
    private static final String ACCEPTED_XO_PROFIT_BTC = "Custom/Accepted/Xo/Closed/ProfitBtc";
    private static final String ACCEPTED_ERR_LOST_BTC = "Custom/Accepted/Xo/Error/LossBtc";

    private final XoTradeRejectedStatTotalRepository rejectedStatRepository;
    private final AcceptedXoTradeRepository xoTradeRepository;
    private final TradeRepository tradeRepository;
    private final WalletRepository walletRepository;
    private final CryptoPricingRepository pricingRepository;

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

        NewRelic.recordMetric(ACCEPTED_UNKNOWN, tradeRepository.countAllByStatusEquals(TradeStatus.UNKNOWN));
        NewRelic.recordMetric(ACCEPTED_OPEN, tradeRepository.countAllByStatusEquals(TradeStatus.OPENED));
        NewRelic.recordMetric(ACCEPTED_CLOSED, tradeRepository.countAllByStatusEquals(TradeStatus.CLOSED));
        NewRelic.recordMetric(ACCEPTED_OTHER, tradeRepository.countAllByStatusNotIn(
                ImmutableSet.of(TradeStatus.UNKNOWN, TradeStatus.OPENED, TradeStatus.CLOSED)
        ));

        Map<TradingCurrency, CryptoPricing> priceList = new EnumMap<>(TradingCurrency.class);
        pricingRepository.findAll().forEach(w -> priceList.put(w.getCurrency(), w));

        reportWalletValue(priceList);
        reportXoPerformanceValue(priceList);
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
        NewRelic.recordMetric(AMOUNT_BTC, btcValue.floatValue());
    }

    private void reportXoPerformanceValue(Map<TradingCurrency, CryptoPricing> priceList) {
        BigDecimal expectedProfitBtc = BigDecimal.ZERO;
        BigDecimal profitBtc = BigDecimal.ZERO;
        BigDecimal errorLossBtc = BigDecimal.ZERO;

        Set<XoAcceptStatus> canOpen = ImmutableSet.of(
                XoAcceptStatus.UNCONFIRMED, XoAcceptStatus.ACK_PART, XoAcceptStatus.ACK_PART, XoAcceptStatus.DONE_PART
        );

        Set<XoAcceptStatus> closed = ImmutableSet.of(
                XoAcceptStatus.DONE, XoAcceptStatus.DONE_BOTH, XoAcceptStatus.REPLENISH
        );

        Set<XoAcceptStatus> failed = ImmutableSet.of(
                XoAcceptStatus.TRADE_ISSUE, XoAcceptStatus.ERROR, XoAcceptStatus.TRANSIENT_ISSUE
        );

        for (AcceptedXoTrade trade : xoTradeRepository.findAll()) {
            CryptoPricing price = priceList.get(trade.getCurrencyFrom());
            if (null == price) {
                continue;
            }

            if (canOpen.contains(trade.getStatus())) {
                expectedProfitBtc = expectedProfitBtc.add(trade.getExpectedProfit().multiply(price.getPriceBtc()));
            } else if (closed.contains(trade.getStatus())) {
                profitBtc = profitBtc.add(trade.getExpectedProfit().multiply(price.getPriceBtc()));
            } else if(failed.contains(trade.getStatus())) {
                errorLossBtc = errorLossBtc.add(trade.getAmount().multiply(price.getPriceBtc()));
            }
        }

        NewRelic.recordMetric(ACCEPTED_XO_CAN_PROFIT_BTC, expectedProfitBtc.floatValue());
        NewRelic.recordMetric(ACCEPTED_XO_PROFIT_BTC, profitBtc.floatValue());
        NewRelic.recordMetric(ACCEPTED_ERR_LOST_BTC, errorLossBtc.floatValue());
    }
}
