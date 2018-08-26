package com.gtc.opportunity.trader.repository.stat;

import com.gtc.opportunity.trader.domain.stat.StaticWallet;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Created by Valentyn Berezin on 27.02.18.
 */
@Repository
public interface StaticWalletRepository extends CrudRepository<StaticWallet, Integer> {

    Collection<StaticWallet> findByBalanceGreaterThan(BigDecimal balance);
}
