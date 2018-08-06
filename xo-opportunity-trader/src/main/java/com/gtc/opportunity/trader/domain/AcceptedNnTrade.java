package com.gtc.opportunity.trader.domain;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Created by Valentyn Berezin on 02.08.18.
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
public class AcceptedNnTrade implements Serializable {

    @Id
    @GeneratedValue
    private int id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "client")
    private Client client;

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
    private BigDecimal expectedDeltaFrom;

    @NotNull
    private BigDecimal expectedDeltaTo;

    private double confidence;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Strategy strategy;

    @NotNull
    @Audited
    @Enumerated(EnumType.STRING)
    private NnAcceptStatus status;

    private LocalDateTime recordedOn;

    @Audited
    private String lastMessageId;

    @Column(name = "model_age_s")
    private int modelAgeS;

    @Column(name = "average_noop_label_age_s")
    private int averageNoopLabelAgeS;

    @Column(name = "average_act_label_age_s")
    private int averageActLabelAgeS;

    private LocalDateTime closedOn;

    @OneToMany(mappedBy = "nnOrder")
    private Collection<Trade> trades;
}
