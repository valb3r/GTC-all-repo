package com.gtc.opportunity.trader.domain.stat.rejected;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Created by Valentyn Berezin on 27.03.18.
 */
@Getter
@Setter
@Embeddable
@EqualsAndHashCode
public class RejectionStat implements Serializable {

    private Double lastThreshold;
    private Double totalThreshold;
    private Double lastValue;
    private Double totalValue;

    private long recordCount;
    private String lastOpportunityId;
    private LocalDateTime lastOpportCreatedOn;
    private LocalDateTime startedOn;

    @Column(name = "max_separation_s")
    private double maxSeparationS;
}
