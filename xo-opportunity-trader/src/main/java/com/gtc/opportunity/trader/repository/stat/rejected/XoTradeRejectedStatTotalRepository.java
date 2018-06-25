package com.gtc.opportunity.trader.repository.stat.rejected;

import com.gtc.opportunity.trader.domain.stat.rejected.XoTradeRejectedStatTotal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Created by Valentyn Berezin on 27.03.18.
 */
@Repository
public interface XoTradeRejectedStatTotalRepository
        extends BaseXoTradeRejectedStatRepository<XoTradeRejectedStatTotal> {

    @Query("SELECT COALESCE(SUM(d.rejections.recordCount), 0) FROM XoTradeRejectedStatDaily d")
    long rejectedCount();

    @Query("SELECT COALESCE(SUM(d.rejections.recordCount), 0) FROM XoTradeRejectedStatDaily d WHERE " +
            "d.id.reason LIKE :likeReason")
    long rejectedCountByLikeReason(@Param("likeReason") String likeReason);
}
