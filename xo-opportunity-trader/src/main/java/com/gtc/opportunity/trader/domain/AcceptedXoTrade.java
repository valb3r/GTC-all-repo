package com.gtc.opportunity.trader.domain;

import com.gtc.meta.TradingCurrency;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Entity
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@DynamicInsert
@DynamicUpdate
public class AcceptedXoTrade implements Serializable {

    @Id
    @GeneratedValue
    private int id;

    @NotNull
    @ManyToOne
    private Client clientFrom;

    @NotNull
    @ManyToOne
    private Client clientTo;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TradingCurrency currencyFrom;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TradingCurrency currencyTo;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private BigDecimal priceFromBuy;

    @NotNull
    private BigDecimal priceToSell;

    @NotNull
    private BigDecimal expectedProfitPct;

    @NotNull
    @Audited
    @Enumerated(EnumType.STRING)
    private XoAcceptStatus status;

    /* Parenting opportunity info */
    @NotNull
    private LocalDateTime opportunityOpenedOn;

    private double opportunityBestSellAmount;
    private double opportunityBestBuyAmount;
    private double opportunityBestSellPrice;
    private double opportunityBestBuyPrice;
    private double opportunityProfitPct;

    private LocalDateTime recordedOn;

    @NotNull
    private BigDecimal expectedProfit;

    @Audited
    private String lastMessageId;
}
