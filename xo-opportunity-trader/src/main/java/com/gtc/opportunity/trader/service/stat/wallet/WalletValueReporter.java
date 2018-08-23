package com.gtc.opportunity.trader.service.stat.wallet;

import com.google.common.collect.ImmutableSet;
import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.CryptoPricing;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.domain.Wallet;
import com.gtc.opportunity.trader.repository.CryptoPricingRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.repository.WalletRepository;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Created by Valentyn Berezin on 04.08.18.
 */
@Service
@RequiredArgsConstructor
public class WalletValueReporter {

    private static final Set<TradeStatus> OPEN = ImmutableSet.of(TradeStatus.OPENED);

    private static final String AMOUNT_RAW = "Custom/Amount/RAW";
    private static final String AMOUNT_USD = "Custom/Amount/USD";
    private static final String AMOUNT_MILLI_BTC = "Custom/Amount/MilliBTC";
    private static final String AMOUNT_WITH_OPEN_MILLI_BTC = "Custom/Amount/WithOpenMilliBTC";

    private final TradeRepository tradeRepository;
    private final WalletRepository walletRepository;
    private final CryptoPricingRepository cryptoPricingRepository;

    @Trace(dispatcher = true)
    @Transactional(readOnly = true)
    @Scheduled(fixedRateString = "#{${app.schedule.reportTradePerformanceS} * 1000}")
    public void reportWalletValue() {
        Map<TradingCurrency, CryptoPricing> priceList = cryptoPricingRepository.priceList();
        BigDecimal rawValue = BigDecimal.ZERO;
        BigDecimal usdValue = BigDecimal.ZERO;
        BigDecimal btcValue = BigDecimal.ZERO;
        BigDecimal btcValueWithOpen = BigDecimal.ZERO;

        for (Wallet wallet : walletRepository.findByBalanceGreaterThan(BigDecimal.ZERO)) {
            rawValue = rawValue.add(wallet.getBalance());
            CryptoPricing pricing = priceList.get(wallet.getCurrency());
            if (null == pricing) {
                continue;
            }

            usdValue = usdValue.add(wallet.getBalance().multiply(pricing.getPriceUsd()));
            BigDecimal walletBtc = wallet.getBalance().multiply(pricing.getPriceBtc());
            btcValue = btcValue.add(walletBtc);
            BigDecimal inBuy = tradeRepository.lockedByBuyTradesWithStatus(wallet, OPEN);
            BigDecimal inSell = tradeRepository.lockedBySellTradesWithStatus(wallet, OPEN);
            btcValueWithOpen = btcValueWithOpen
                    .add(walletBtc.add((inBuy.add(inSell)).multiply(pricing.getPriceBtc())));
        }

        NewRelic.recordMetric(AMOUNT_RAW, rawValue.floatValue());
        NewRelic.recordMetric(AMOUNT_USD, usdValue.floatValue());
        NewRelic.recordMetric(AMOUNT_MILLI_BTC, btcValue.floatValue() * 1000.0f);
        NewRelic.recordMetric(AMOUNT_WITH_OPEN_MILLI_BTC, btcValueWithOpen.floatValue() * 1000.0f);
    }
}
