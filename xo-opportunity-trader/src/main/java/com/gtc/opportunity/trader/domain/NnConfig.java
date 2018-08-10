package com.gtc.opportunity.trader.domain;

import com.google.common.base.Objects;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 01.08.18.
 */
@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class NnConfig implements Serializable {

    @Id
    private int id;

    @MapsId
    @OneToOne
    @JoinColumn(name = "id")
    private ClientConfig clientCfg;

    @Column(name = "future_n_window")
    private int futureNwindow;

    @Column(name = "collect_n_labeled")
    private int collectNlabeled;

    private BigDecimal noopThreshold;
    private BigDecimal truthThreshold;
    private BigDecimal proceedFalsePositive;

    @Column(name = "average_dt_s_between_labels")
    private BigDecimal averageDtSBetweenLabels;

    @Column(name = "book_test_for_open_per_s")
    private BigDecimal bookTestForOpenPerS;

    @Column(name = "old_threshold_m")
    private int oldThresholdM;

    @Column(name = "n_train_iterations")
    private int nTrainIterations;

    private BigDecimal trainRelativeSize;

    @Lob
    private String networkYamlSpec;

    private BigDecimal futurePriceGainPct;

    @Column(name = "expire_open_h")
    private int expireOpenH;

    private boolean enabled;

    @Column(name = "max_slave_delay_m")
    private int maxSlaveDelayM;

    public int hashValue() {
        return Objects.hashCode(futureNwindow, collectNlabeled, noopThreshold, truthThreshold, proceedFalsePositive,
                averageDtSBetweenLabels, bookTestForOpenPerS, oldThresholdM, nTrainIterations,
                trainRelativeSize, networkYamlSpec, futurePriceGainPct, expireOpenH, maxSlaveDelayM);
    }
}
