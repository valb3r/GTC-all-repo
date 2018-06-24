package com.gtc.opportunity.trader.repository;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.AcceptedXoTrade;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Created by Valentyn Berezin on 25.02.18.
 */
@Repository
public interface AcceptedXoTradeRepository extends CrudRepository<AcceptedXoTrade, Integer> {

    @Query("SELECT COUNT(xo) FROM AcceptedXoTrade xo WHERE " +
            "xo.clientFrom.name = :clientFrom AND xo.clientTo.name = :clientTo "
            + "AND xo.currencyFrom = :currencyFrom "
            + "AND xo.currencyTo = :currencyTo AND xo.recordedOn >= :recordedOn")
    int countByKeyOlderThan(@Param("clientFrom") String clientFrom,
                            @Param("clientTo") String clientTo,
                             @Param("currencyFrom") TradingCurrency currencyFrom,
                             @Param("currencyTo") TradingCurrency currencyTo,
                             @Param("recordedOn") LocalDateTime recordedOn);
}
