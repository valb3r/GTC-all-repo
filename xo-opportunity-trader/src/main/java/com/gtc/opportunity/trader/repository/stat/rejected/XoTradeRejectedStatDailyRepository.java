package com.gtc.opportunity.trader.repository.stat.rejected;

import com.gtc.opportunity.trader.domain.stat.rejected.XoTradeRejectedStatDaily;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Created by Valentyn Berezin on 27.03.18.
 */
@Repository
public interface XoTradeRejectedStatDailyRepository
        extends BaseXoTradeRejectedStatRepository<XoTradeRejectedStatDaily> {

    @Query("SELECT SUM(d.rejections.recordCount) FROM XoTradeRejectedStatDaily d WHERE d.kind = 'TOTAL'")
    Long rejectedCount();

    @Query("SELECT SUM(d.rejections.recordCount) FROM XoTradeRejectedStatDaily d WHERE d.kind = 'TOTAL' " +
            "AND d.id.reason LIKE :likeReason")
    Long rejectedCountByLikeReason(@Param("likeReason") String likeReason);
}
