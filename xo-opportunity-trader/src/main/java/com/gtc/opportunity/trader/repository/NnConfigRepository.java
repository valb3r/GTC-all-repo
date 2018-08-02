package com.gtc.opportunity.trader.repository;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.NnConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Repository
public interface NnConfigRepository extends CrudRepository<NnConfig, Integer> {

    @Query("SELECT nn FROM NnConfig nn JOIN nn.clientCfg cc " +
            "WHERE cc.client.name = :clientName " +
            "AND cc.currency = :currencyFrom AND cc.currencyTo = :currencyTo " +
            "AND cc.client.enabled = TRUE AND cc.enabled = TRUE AND nn.enabled = TRUE")
    Optional<NnConfig> findActiveByKey(@Param("clientName") String clientName,
                                       @Param("currencyFrom") TradingCurrency currencyFrom,
                                       @Param("currencyTo") TradingCurrency currencyTo);

    @Query("SELECT nn FROM NnConfig nn JOIN nn.clientCfg cc " +
            "WHERE cc.client.enabled = TRUE AND cc.enabled = TRUE AND nn.enabled = TRUE")
    List<NnConfig> findAllActive();
}
