package com.gtc.opportunity.trader.domain.stat.rejected;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Created by Valentyn Berezin on 27.03.18.
 */
@Entity
@DiscriminatorValue("DAILY")
@DynamicInsert
@DynamicUpdate
public class XoTradeRejectedStatDaily extends BaseXoTradeRejectedStat {
}
