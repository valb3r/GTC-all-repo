package com.gtc.opportunity.trader.repository.stat.rejected;

import com.gtc.opportunity.trader.domain.stat.RejectedId;
import com.gtc.opportunity.trader.domain.stat.rejected.BaseXoTradeRejectedStat;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by Valentyn Berezin on 27.03.18.
 */
public interface BaseXoTradeRejectedStatRepository<T extends BaseXoTradeRejectedStat>
        extends CrudRepository<T, RejectedId> {
}
