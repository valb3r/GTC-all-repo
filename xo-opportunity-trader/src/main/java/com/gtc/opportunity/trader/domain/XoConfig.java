package com.gtc.opportunity.trader.domain;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 01.08.18.
 */
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class XoConfig implements Serializable {

    @Id
    private int id;

    @MapsId
    @OneToOne
    @JoinColumn(name = "id")
    private ClientConfig clientCfg;

    @NotNull
    private BigDecimal minProfitabilityPct;

    /* Amount of opportunity reported to use, %*/
    private BigDecimal safetyMarginAmountPct;

    /* Maximum safety deviation from price in range best price (0) - non-profit price (100), % */
    private BigDecimal safetyMarginPricePct;

    /* Calculated profit after safety measures should be greater than this, %*/
    private BigDecimal requiredProfitablityPct;

    private double xoRatePerSec;

    private int maxSolveTimeMs;

    private boolean isReplenishable;

    private int maxSolveReplenishTimeMs;

    @Column(name = "max_solve_rate_per_s")
    private int maxSolveRatePerS;

    @Column(name = "stale_book_threshold_ms")
    private int staleBookThresholdMS;

    private BigDecimal singleSideTradeLimit;

    private boolean enabled;
}
