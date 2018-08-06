package com.gtc.opportunity.trader.repository;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.XoConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Repository
public interface XoConfigRepository extends CrudRepository<XoConfig, Integer> {

    @Query("SELECT xo FROM XoConfig xo JOIN xo.clientCfg cc " +
            "WHERE cc.client.name = :clientName " +
            "AND cc.currency = :currencyFrom AND cc.currencyTo = :currencyTo " +
            "AND cc.client.enabled = TRUE AND cc.enabled = TRUE AND xo.enabled = TRUE")
    Optional<XoConfig> findActiveByKey(@Param("clientName") String clientName,
                                       @Param("currencyFrom") TradingCurrency currencyFrom,
                                       @Param("currencyTo") TradingCurrency currencyTo);
}
