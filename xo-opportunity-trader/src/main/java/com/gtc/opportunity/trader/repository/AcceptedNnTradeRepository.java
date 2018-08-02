package com.gtc.opportunity.trader.repository;

import com.gtc.opportunity.trader.domain.AcceptedNnTrade;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by Valentyn Berezin on 02.08.18.
 */
@Repository
public interface AcceptedNnTradeRepository extends CrudRepository<AcceptedNnTrade, Integer> {
}
