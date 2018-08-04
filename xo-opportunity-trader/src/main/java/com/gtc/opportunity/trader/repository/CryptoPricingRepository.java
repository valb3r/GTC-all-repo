package com.gtc.opportunity.trader.repository;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.CryptoPricing;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by Valentyn Berezin on 25.06.18.
 */
@Repository
public interface CryptoPricingRepository extends CrudRepository<CryptoPricing, TradingCurrency> {

    default Map<TradingCurrency, CryptoPricing> priceList() {
        return StreamSupport.stream(findAll().spliterator(), false).collect(
                Collectors.toMap(CryptoPricing::getCurrency, Function.identity())
        );
    }
}
