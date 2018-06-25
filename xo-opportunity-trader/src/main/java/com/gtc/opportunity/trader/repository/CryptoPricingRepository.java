package com.gtc.opportunity.trader.repository;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.CryptoPricing;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by Valentyn Berezin on 25.06.18.
 */
@Repository
public interface CryptoPricingRepository extends CrudRepository<CryptoPricing, TradingCurrency> {
}
