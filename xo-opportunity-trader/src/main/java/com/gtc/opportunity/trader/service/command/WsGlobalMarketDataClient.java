package com.gtc.opportunity.trader.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.provider.SubscribeMarketPricesDto;
import com.gtc.opportunity.trader.config.WsConfig;
import com.gtc.opportunity.trader.repository.WalletRepository;
import com.gtc.opportunity.trader.service.nnopportunity.NnDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by Valentyn Berezin on 16.06.18.
 */
@Slf4j
@Service
public class WsGlobalMarketDataClient extends BaseWsProviderClient {

    public WsGlobalMarketDataClient(
            WsConfig wsConfig,
            ObjectMapper objectMapper,
            WalletRepository wallets,
            NnDispatcher dispatcher) {
        super(
                "wsGlobalMarket",
                wsConfig,
                objectMapper,
                () -> StreamSupport.stream(wallets.findAll().spliterator(), false)
                        .map(it -> new SubscribeMarketPricesDto(
                                it.getCurrency(),
                                ImmutableSet.of(TradingCurrency.Bitcoin, TradingCurrency.Usd)))
                        .collect(Collectors.toList()),
                dispatcher::acceptOrderBook
        );
    }
}
