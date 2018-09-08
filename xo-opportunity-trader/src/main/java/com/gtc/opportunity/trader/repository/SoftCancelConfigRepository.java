package com.gtc.opportunity.trader.repository;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.SoftCancelConfig;
import com.gtc.opportunity.trader.domain.Trade;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Created by Valentyn Berezin on 02.08.18.
 */
@Repository
public interface SoftCancelConfigRepository extends CrudRepository<SoftCancelConfig, Integer> {

    @Query("SELECT sc FROM SoftCancelConfig sc JOIN sc.clientCfg cc " +
            "WHERE cc.client.enabled = TRUE AND cc.enabled = TRUE AND sc.enabled = TRUE")
    List<SoftCancelConfig> findAllActive();

    @Query("SELECT sc FROM SoftCancelConfig sc " +
            "WHERE sc.clientCfg.client.name = :name " +
            "AND sc.clientCfg.currency = :currFrom " +
            "AND sc.clientCfg.currencyTo = :currTo")
    Optional<SoftCancelConfig> findForKey(@Param("name") String name,
                                    @Param("currFrom") TradingCurrency currencyFrom,
                                    @Param("currTo") TradingCurrency currencyTo);

    default Optional<SoftCancelConfig> findForTrade(Trade trade) {
        return findForKey(trade.getClient().getName(), trade.getCurrencyFrom(), trade.getCurrencyTo());
    }
}
