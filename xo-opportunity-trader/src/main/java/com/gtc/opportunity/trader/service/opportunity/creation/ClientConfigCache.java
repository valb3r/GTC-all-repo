package com.gtc.opportunity.trader.service.opportunity.creation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.config.CacheConfig;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.repository.ClientConfigRepository;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Valentyn Berezin on 27.06.18.
 */
@Component
public class ClientConfigCache {

    private final ClientConfigRepository cfgRepository;

    private final Cache<String, Optional<ClientConfig>> cfgCache;

    public ClientConfigCache(ClientConfigRepository cfgRepository, CacheConfig config) {
        this.cfgRepository = cfgRepository;

        this.cfgCache = CacheBuilder.newBuilder()
                .maximumSize(config.getCfgCache().getSize())
                .expireAfterWrite(config.getCfgCache().getLiveS(), TimeUnit.SECONDS)
                .build();
    }

    @SneakyThrows
    @Transactional(readOnly = true)
    public Optional<ClientConfig> getCfg(String clientName, TradingCurrency from, TradingCurrency to) {
        return cfgCache.get(
                clientName + from.toString() + to.toString(),
                () -> cfgRepository.findActiveByKey(clientName, from, to)
        );
    }
}
