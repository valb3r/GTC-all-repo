package com.gtc.opportunity.trader.domain.stat.general;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by Valentyn Berezin on 26.02.18.
 */
@Getter
@Setter
@Embeddable
@EqualsAndHashCode
public class Snapshot implements Serializable {

    private double minHistWin = Double.MAX_VALUE;

    @NotNull
    private BigDecimal totHistWin = new BigDecimal(0);

    private double maxHistWin;

    private double maxTickerWin;

    @Column(name = "min_duration_s")
    private double minDurationS = Double.MAX_VALUE;

    @NotNull
    @Column(name = "tot_duration_s")
    private BigDecimal totDurationS = new BigDecimal(0);

    @Column(name = "max_duration_s")
    private double maxDurationS;

    private double minSellAmount = Double.MAX_VALUE;

    @NotNull
    private BigDecimal totSellAmount = new BigDecimal(0);

    private double maxSellAmount;

    private double minBuyAmount = Double.MAX_VALUE;

    @NotNull
    private BigDecimal totBuyAmount = new BigDecimal(0);

    private double maxBuyAmount;
    private long recordCount;
    private String lastOpportunityId;
    private LocalDateTime lastOpportCreatedOn;
    private LocalDateTime startedOn;

    @Column(name = "max_separation_s")
    private double maxSeparationS;
}
