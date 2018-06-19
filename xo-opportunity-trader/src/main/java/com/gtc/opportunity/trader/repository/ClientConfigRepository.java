package com.gtc.opportunity.trader.repository;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.ClientConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Repository
public interface ClientConfigRepository extends CrudRepository<ClientConfig, Integer> {

    @Query("SELECT cc FROM ClientConfig cc WHERE cc.client.name = :clientName " +
            "AND cc.currency = :currencyFrom AND cc.currencyTo = :currencyTo " +
            "AND cc.client.enabled = true")
    Optional<ClientConfig> findActiveByKey(@Param("clientName") String clientName,
                                           @Param("currencyFrom") TradingCurrency currencyFrom,
                                           @Param("currencyTo") TradingCurrency currencyTo);
}
