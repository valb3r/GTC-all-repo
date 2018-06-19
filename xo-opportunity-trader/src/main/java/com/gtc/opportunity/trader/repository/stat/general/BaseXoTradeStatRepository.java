package com.gtc.opportunity.trader.repository.stat.general;

import com.gtc.opportunity.trader.domain.stat.general.BaseXoTradeStat;
import com.gtc.opportunity.trader.domain.stat.StatId;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by Valentyn Berezin on 26.02.18.
 */
public interface BaseXoTradeStatRepository<T extends BaseXoTradeStat> extends CrudRepository<T, StatId> {
}
