package com.gtc.opportunity.trader.domain;

import com.gtc.meta.TradingCurrency;
import lombok.*;

import javax.persistence.*;
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
@EqualsAndHashCode(of = "currencyTo", callSuper = true)
public class ClientConfig extends ByClientAndCurrency {

    @Enumerated(EnumType.STRING)
    private TradingCurrency currencyTo;

    @NotNull
    private BigDecimal minOrder;

    @NotNull
    private BigDecimal maxOrder;

    private BigDecimal minOrderInToCurrency;

    @NotNull
    private BigDecimal tradeChargeRatePct;

    @Min(1)
    private int scalePrice;

    @Min(1)
    private int scaleAmount;

    private boolean enabled = true;

    @OneToOne(mappedBy = "clientCfg")
    private XoConfig xoConfig;

    @OneToOne(mappedBy = "clientCfg")
    private NnConfig nnConfig;

    @OneToOne(mappedBy = "clientCfg")
    private SoftCancelConfig softCancelConfig;

    @Enumerated(EnumType.STRING)
    private FeeSystem feeSystem;

    @Builder(toBuilder = true)
    public ClientConfig(int id, Client client, TradingCurrency currency, TradingCurrency currencyTo,
                        BigDecimal minOrder, BigDecimal maxOrder, BigDecimal minOrderInToCurrency,
                        BigDecimal tradeChargeRatePct, int scalePrice, int scaleAmount) {
        super(id, client, currency);
        this.currencyTo = currencyTo;
        this.minOrder = minOrder;
        this.maxOrder = maxOrder;
        this.minOrderInToCurrency = minOrderInToCurrency;
        this.tradeChargeRatePct = tradeChargeRatePct;
        this.scalePrice = scalePrice;
        this.scaleAmount = scaleAmount;
    }
}
