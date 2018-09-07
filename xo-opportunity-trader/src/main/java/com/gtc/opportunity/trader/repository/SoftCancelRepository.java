package com.gtc.opportunity.trader.repository;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.SoftCancel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created by Valentyn Berezin on 02.08.18.
 */
@Repository
public interface SoftCancelRepository extends CrudRepository<SoftCancel, Integer> {

    @Query("SELECT sc FROM SoftCancel sc " +
            "WHERE sc.clientCfg.client.name = :name AND sc.clientCfg.currency = :currFrom " +
            "AND sc.clientCfg.currencyTo = :currTo")
    Optional<SoftCancel> findForTrade(@Param("name") String name,
                                      @Param("currFrom") TradingCurrency currencyFrom,
                                      @Param("currTo") TradingCurrency currencyTo);
}
