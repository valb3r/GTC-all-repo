package com.gtc.opportunity.trader.service.trade.management;

import com.google.common.collect.ImmutableMap;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.provider.MarketPrice;
import com.gtc.opportunity.trader.domain.CryptoPricing;
import com.gtc.opportunity.trader.repository.CryptoPricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created by Valentyn Berezin on 25.08.18.
 */
@Service
@RequiredArgsConstructor
public class ActualPriceUpdater {

    private final Map<TradingCurrency, BiConsumer<CryptoPricing, BigDecimal>> handlers = ImmutableMap.of(
            TradingCurrency.Bitcoin, this::updateBtc, TradingCurrency.Usd, this::updateUsd
    );

    private final CryptoPricingRepository pricingRepository;

    @Transactional
    public void updatePrice(MarketPrice price) {
        if (!handlers.containsKey(price.getTo())) {
            return;
        }

        pricingRepository.findById(price.getFrom()).ifPresent(dbPrice ->
                handlers.get(price.getTo()).accept(dbPrice, price.getPrice())
        );
    }

    private void updateBtc(CryptoPricing pricing, BigDecimal price) {
        pricing.setPriceBtc(price);
        pricing.setUpdatedAt(null);
        pricingRepository.save(pricing);
    }

    private void updateUsd(CryptoPricing pricing, BigDecimal price) {
        pricing.setPriceUsd(price);
        pricing.setUpdatedAt(null);
        pricingRepository.save(pricing);
    }
}
