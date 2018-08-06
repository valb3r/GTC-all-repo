package com.gtc.opportunity.trader.domain;

import com.gtc.meta.TradingCurrency;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by Valentyn Berezin on 25.06.18.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "currency")
public class CryptoPricing {

    @Id
    @NotNull
    @Enumerated(EnumType.STRING)
    private TradingCurrency currency;

    @NotNull
    private BigDecimal priceUsd;

    @NotNull
    private BigDecimal priceBtc;

    private LocalDateTime updatedAt;
}
