package com.gtc.opportunity.trader.domain.stat.rejected;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Created by Valentyn Berezin on 27.03.18.
 */
@Entity
@DiscriminatorValue("DAILY")
public class XoTradeRejectedStatDaily extends BaseXoTradeRejectedStat {
}
