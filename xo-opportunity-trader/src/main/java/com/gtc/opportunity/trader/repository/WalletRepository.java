package com.gtc.opportunity.trader.repository;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.Client;
import com.gtc.opportunity.trader.domain.Wallet;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

/**
 * Created by Valentyn Berezin on 27.02.18.
 */
@Repository
public interface WalletRepository extends CrudRepository<Wallet, Integer> {

    Optional<Wallet> findByClientAndCurrency(Client client, TradingCurrency currency);

    Collection<Wallet> findByBalanceGreaterThan(BigDecimal balance);
}
