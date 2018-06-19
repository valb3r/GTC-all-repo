package com.gtc.opportunity.trader.cqe.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Created by Valentyn Berezin on 24.03.18.
 */
@Data
@Builder
@Embeddable
@AllArgsConstructor
public class Statistic implements Serializable {

    private double min;
    private double max;
    private double total;
    private double curr;

    public Statistic(double curr) {
        this.min = curr;
        this.max = curr;
        this.total = curr;
        this.curr = curr;
    }

    public void update(double val) {
        min = Math.min(min, val);
        max = Math.max(max, val);
        total += val;
        curr = val;
    }

    public void mergeAsPrimary(Statistic other) {
        min = Math.min(min, other.min);
        max = Math.max(max, other.max);
        total += other.total;
    }
}
