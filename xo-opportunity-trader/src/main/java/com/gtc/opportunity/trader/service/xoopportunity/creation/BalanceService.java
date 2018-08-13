package com.gtc.opportunity.trader.service.xoopportunity.creation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.config.CacheConfig;
import com.gtc.opportunity.trader.domain.Client;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.Wallet;
import com.gtc.opportunity.trader.repository.WalletRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Valentyn Berezin on 25.02.18.
 */
@Slf4j
@Service
public class BalanceService {

    private static final String BROKEN_CACHE = "Broken wallet cache";

    private final ConfigCache cfgCache;
    private final EntityManager entityManager;
    private final WalletRepository walletRepository;

    // cache just to check that wallet exists (wallet can exist if it has some balance or was created before_
    private final Cache<String, Optional<Integer>> walletIds;

    public BalanceService(ConfigCache cfgCache, EntityManager entityManager, WalletRepository walletRepository,
                          CacheConfig config) {
        this.cfgCache = cfgCache;
        this.entityManager = entityManager;
        this.walletRepository = walletRepository;
        walletIds = CacheBuilder.newBuilder()
                .maximumSize(config.getWalletIds().getSize())
                .expireAfterWrite(config.getWalletIds().getLiveS(), TimeUnit.SECONDS)
                .build();
    }

    @Transactional(readOnly = true)
    public boolean canProceed(Trade trade, boolean reservationCommitted) {
        TradingCurrency charged = chargedWalletCurrency(trade);
        ClientConfig config = cfgCache.getClientCfg(trade)
                .orElseThrow(() -> new IllegalStateException(BROKEN_CACHE));
        if (!fetchWallet(trade.getClient(), chargedWalletCurrency(trade)).isPresent()) {
            return false;
        }

        Wallet wallet = walletRepository.findByClientAndCurrency(trade.getClient(), charged)
                .orElseThrow(() -> new IllegalStateException(BROKEN_CACHE));

        BigDecimal reserved = wallet.getReservedBalance();
        BigDecimal masterReturn = null == trade.getDependsOn() ?
                BigDecimal.ZERO : tradeAmount(trade.getDependsOn(), charged)
                .multiply(BigDecimal.ONE.subtract(config.getTradeChargeRatePct().movePointLeft(2)));

        if (reservationCommitted) {
            // for dependent just check it has enough balance right now
            BigDecimal tradeAmount = tradeAmount(trade, charged);
            return wallet.getBalance().compareTo(tradeAmount) >= 0;
        }

        return wallet.getBalance().add(masterReturn).subtract(reserved).compareTo(BigDecimal.ZERO) >= 0;
    }

    private static BigDecimal tradeAmount(Trade trade, TradingCurrency charged) {
        if (charged.equals(trade.getCurrencyFrom())) {
            return trade.getOpeningAmount().abs();
        }
        return trade.getOpeningAmount().abs().multiply(trade.getPrice());
    }

    @Transactional(readOnly = true)
    public void assignWallet(Trade trade) {
        int walletId = fetchWallet(trade.getClient(), chargedWalletCurrency(trade))
                .orElseThrow(() -> new IllegalStateException(BROKEN_CACHE));
        trade.setWallet(entityManager.getReference(Wallet.class, walletId));
    }

    @SneakyThrows
    private Optional<Integer> fetchWallet(Client client, TradingCurrency currency) {
        return walletIds.get(
                walletKey(client, currency),
                () -> walletRepository.findByClientAndCurrency(client, currency)
                        .map(Wallet::getId)
        );
    }

    private static String walletKey(Client client, TradingCurrency currency) {
        return client.getName() + currency.toString();
    }

    private static TradingCurrency chargedWalletCurrency(Trade trade) {
        if (trade.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            return trade.getCurrencyFrom();
        }

        return trade.getCurrencyTo();
    }
}
