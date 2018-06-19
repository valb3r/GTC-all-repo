package com.gtc.opportunity.trader.service.opportunity.creation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.config.CacheConfig;
import com.gtc.opportunity.trader.domain.Client;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.Wallet;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.repository.WalletRepository;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Valentyn Berezin on 25.02.18.
 */
@Service
public class BalanceService {

    private static final String BROKEN_CACHE = "Broken wallet cache";

    private final EntityManager entityManager;
    private final WalletRepository walletRepository;
    private final TradeRepository tradeRepository;

    // cache just to check that wallet exists (wallet can exist if it has some balance or was created before_
    private final Cache<String, Optional<Integer>> walletIds;

    public BalanceService(EntityManager entityManager, WalletRepository walletRepository,
                          TradeRepository tradeRepository, CacheConfig config) {
        this.entityManager = entityManager;
        this.walletRepository = walletRepository;
        this.tradeRepository = tradeRepository;
        walletIds = CacheBuilder.newBuilder()
                .maximumSize(config.getWalletIds().getSize())
                .expireAfterWrite(config.getWalletIds().getLiveS(), TimeUnit.SECONDS)
                .build();
    }

    @Transactional(readOnly = true)
    public boolean canProceed(Trade trade) {
        TradingCurrency charged = chargedWalletCurrency(trade);
        if (!fetchWallet(trade.getClient(), chargedWalletCurrency(trade)).isPresent()) {
            return false;
        }

        Wallet wallet = walletRepository.findByClientAndCurrency(trade.getClient(), charged)
                .orElseThrow(() -> new IllegalStateException(BROKEN_CACHE));

        BigDecimal reserved = walletReservedByTrades(wallet);

        if (charged.equals(trade.getCurrencyFrom())) {
            return wallet.getBalance()
                    .subtract(reserved)
                    .compareTo(trade.getAmount().abs()) >= 0;
        }

        return wallet.getBalance().subtract(reserved)
                .compareTo(trade.getAmount().abs().multiply(trade.getPrice())) >= 0;
    }

    @Transactional(readOnly = true)
    public void proceed(Trade trade) {
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

    private BigDecimal walletReservedByTrades(Wallet wallet) {
        // any order after last wallet update should be subtracted from balance:
        Collection<Trade> openTradesNotInBal = tradeRepository.findByWalletKeyUpdatedAfter(
                wallet.getClient(),
                wallet.getCurrency(),
                wallet.getStatusUpdated());

        return openTradesNotInBal.stream()
                .map(it -> it.amountReservedOnWallet(wallet)
                        .orElseThrow(() -> new IllegalStateException("Wrong query result")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
