package com.gtc.opportunity.trader.domain;

import com.gtc.meta.TradingCurrency;
import lombok.*;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Wallet extends ByClientAndCurrency {

    @Audited
    private BigDecimal balance;

    private LocalDateTime statusUpdated;

    @Version
    private int version;

    @Builder
    public Wallet(int id, Client client, TradingCurrency currency, BigDecimal balance, LocalDateTime statusUpdated) {
        super(id, client, currency);
        this.balance = balance;
        this.statusUpdated = statusUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wallet)) return false;
        if (!super.equals(o)) return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
