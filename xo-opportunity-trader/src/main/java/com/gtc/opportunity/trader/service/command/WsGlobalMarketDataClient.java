package com.gtc.opportunity.trader.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.provider.MarketPrice;
import com.gtc.model.provider.SubscribeMarketPricesDto;
import com.gtc.opportunity.trader.config.WsConfig;
import com.gtc.opportunity.trader.repository.WalletRepository;
import com.gtc.opportunity.trader.service.trade.management.ActualPriceUpdater;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 16.06.18.
 */
@Slf4j
@Service
public class WsGlobalMarketDataClient extends BaseWsProviderClient<MarketPrice> {

    public WsGlobalMarketDataClient(
            WsConfig wsConfig,
            ObjectMapper objectMapper,
            WalletRepository wallets,
            ActualPriceUpdater dispatcher) {
        super(
                "wsGlobalMarket",
                wsConfig,
                objectMapper,
                () -> wallets.findByBalanceGreaterThan(BigDecimal.ZERO).stream()
                        .map(it -> new SubscribeMarketPricesDto(
                                it.getCurrency(),
                                ImmutableSet.of(TradingCurrency.Bitcoin, TradingCurrency.Usd)))
                        .collect(Collectors.toList()),
                dispatcher::updatePrice,
                MarketPrice.class
        );
    }
}
