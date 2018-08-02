package com.gtc.opportunity.trader.service.opportunity.creation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.config.CacheConfig;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.domain.XoConfig;
import com.gtc.opportunity.trader.repository.ClientConfigRepository;
import com.gtc.opportunity.trader.repository.NnConfigRepository;
import com.gtc.opportunity.trader.repository.XoConfigRepository;
import com.gtc.opportunity.trader.service.nnopportunity.dto.Snapshot;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Valentyn Berezin on 27.06.18.
 */
@Component
public class ConfigCache {

    private final ClientConfigRepository clientCfgRepository;
    private final NnConfigRepository nnCfgRepository;
    private final XoConfigRepository xoCfgRepository;

    private final Cache<String, Optional<ClientConfig>> cfgCache;
    private final Cache<String, Optional<NnConfig>> nnCache;
    private final Cache<String, Optional<XoConfig>> xoCache;

    public ConfigCache(ClientConfigRepository clientCfgRepository,
                       NnConfigRepository nnCfgRepository,
                       XoConfigRepository xoCfgRepository,
                       CacheConfig config) {
        this.clientCfgRepository = clientCfgRepository;
        this.nnCfgRepository = nnCfgRepository;
        this.xoCfgRepository = xoCfgRepository;

        this.cfgCache = buildCache(config);
        this.nnCache =  buildCache(config);
        this.xoCache =  buildCache(config);
    }

    @SneakyThrows
    @Transactional(readOnly = true)
    public Optional<ClientConfig> getClientCfg(String clientName, TradingCurrency from, TradingCurrency to) {
        return cfgCache.get(
                computeKey(clientName, from ,to),
                () -> clientCfgRepository.findActiveByKey(clientName, from, to)
        );
    }

    @SneakyThrows
    @Transactional(readOnly = true)
    public Optional<NnConfig> getNnCfg(String clientName, TradingCurrency from, TradingCurrency to) {
        return nnCache.get(
                computeKey(clientName, from ,to),
                () -> nnCfgRepository.findActiveByKey(clientName, from, to)
        );
    }

    @SneakyThrows
    @Transactional(readOnly = true)
    public Optional<XoConfig> getXoCfg(String clientName, TradingCurrency from, TradingCurrency to) {
        return xoCache.get(
                computeKey(clientName, from ,to),
                () -> xoCfgRepository.findActiveByKey(clientName, from, to)
        );
    }

    @Transactional(readOnly = true)
    public Optional<NnConfig> readConfig(OrderBook book) {
        return getNnCfg(
                book.getMeta().getClient(),
                book.getMeta().getPair().getFrom(),
                book.getMeta().getPair().getTo()
        );
    }

    @Transactional(readOnly = true)
    public NnConfig requireConfig(Snapshot snapshot) {
        return getNnCfg(
                snapshot.getKey().getClient(),
                snapshot.getKey().getPair().getFrom(),
                snapshot.getKey().getPair().getTo()
        ).orElseThrow(() -> new IllegalStateException("No config"));
    }

    @Transactional(readOnly = true)
    public NnConfig requireConfig(OrderBook book) {
        return readConfig(book).orElseThrow(() -> new IllegalStateException("No config"));
    }

    private static String computeKey(String clientName, TradingCurrency from, TradingCurrency to) {
        return clientName + from.toString() + to.toString();
    }

    private static <T> Cache<String, Optional<T>> buildCache(CacheConfig config) {
        return CacheBuilder.newBuilder()
                .maximumSize(config.getCfgCache().getSize())
                .expireAfterWrite(config.getCfgCache().getLiveS(), TimeUnit.SECONDS)
                .build();
    }
}
