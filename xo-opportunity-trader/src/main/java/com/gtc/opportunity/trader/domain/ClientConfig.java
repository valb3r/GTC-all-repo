package com.gtc.opportunity.trader.domain;

import com.gtc.meta.TradingCurrency;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Entity
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class ClientConfig extends ByClientAndCurrency {

    @Enumerated(EnumType.STRING)
    private TradingCurrency currencyTo;

    @NotNull
    private BigDecimal minProfitabilityPct;

    @NotNull
    private BigDecimal minOrder;

    @NotNull
    private BigDecimal maxOrder;

    private BigDecimal minOrderInToCurrency;

    /* Amount of opportunity reported to use, %*/
    private BigDecimal safetyMarginAmountPct;

    /* Maximum safety deviation from price in range best price (0) - non-profit price (100), % */
    private BigDecimal safetyMarginPricePct;

    /* Calculated profit after safety measures should be greater than this, %*/
    private BigDecimal requiredProfitablityPct;

    private double xoRatePerSec;

    @NotNull
    private BigDecimal tradeChargeRatePct;

    @Min(1)
    private int scalePrice;

    @Min(1)
    private int scaleAmount;

    private int maxSolveTimeMs;

    private boolean isReplenishable;

    private int maxSolveReplenishTimeMs;

    @Column(name = "max_solve_rate_per_s")
    private int maxSolveRatePerS;

    @Builder(toBuilder = true)
    public ClientConfig(int id, Client client, TradingCurrency currency, TradingCurrency currencyTo,
                        BigDecimal minProfitabilityPct, BigDecimal minOrder, BigDecimal maxOrder,
                        BigDecimal minOrderInToCurrency, BigDecimal safetyMarginAmountPct,
                        BigDecimal safetyMarginPricePct, BigDecimal requiredProfitablityPct, double xoRatePerSec,
                        BigDecimal tradeChargeRatePct, int scalePrice, int scaleAmount,
                        int maxSolveTimeMs, boolean isReplenishable) {
        super(id, client, currency);
        this.currencyTo = currencyTo;
        this.minProfitabilityPct = minProfitabilityPct;
        this.minOrder = minOrder;
        this.maxOrder = maxOrder;
        this.minOrderInToCurrency = minOrderInToCurrency;
        this.safetyMarginAmountPct = safetyMarginAmountPct;
        this.safetyMarginPricePct = safetyMarginPricePct;
        this.requiredProfitablityPct = requiredProfitablityPct;
        this.xoRatePerSec = xoRatePerSec;
        this.tradeChargeRatePct = tradeChargeRatePct;
        this.scalePrice = scalePrice;
        this.scaleAmount = scaleAmount;
        this.maxSolveTimeMs = maxSolveTimeMs;
        this.isReplenishable = isReplenishable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientConfig)) return false;
        if (!super.equals(o)) return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
