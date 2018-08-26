package com.gtc.opportunity.trader.domain;

import com.gtc.meta.TradingCurrency;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class ByClientAndCurrency implements Serializable {

    @Id
    @GeneratedValue
    protected int id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    protected Client client;

    @Enumerated(EnumType.STRING)
    protected TradingCurrency currency;
}
