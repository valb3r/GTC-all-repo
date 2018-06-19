package com.gtc.opportunity.trader.domain.stat;

import lombok.*;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Created by Valentyn Berezin on 03.03.18.
 */
@Getter
@Setter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class StatId implements Serializable {

    @Embedded
    private MainKey mainKey;

    @NotNull
    private LocalDate sinceDate;

    @Getter
    @Setter
    @Embeddable
    @Builder
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitGroup implements Serializable {

        @NotNull
        private BigDecimal profitGroupPctMin;

        @NotNull
        private BigDecimal profitGroupPctMax;
    }
}
