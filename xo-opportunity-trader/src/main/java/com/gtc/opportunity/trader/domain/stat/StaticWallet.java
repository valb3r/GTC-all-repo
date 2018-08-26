package com.gtc.opportunity.trader.domain.stat;

import com.gtc.opportunity.trader.domain.ByClientAndCurrency;
import lombok.*;

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
public class StaticWallet extends ByClientAndCurrency {

    private BigDecimal balance;

    private LocalDateTime statusUpdated;

    @Version
    private int version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StaticWallet)) return false;
        if (!super.equals(o)) return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
