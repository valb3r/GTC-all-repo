package com.gtc.opportunity.trader.domain.stat;

import com.gtc.meta.TradingCurrency;
import lombok.*;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Valentyn Berezin on 27.03.18.
 */
@Getter
@Setter
@Embeddable
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class MainKey implements Serializable {

    @NotNull
    private String clientFromName;

    @NotNull
    private String clientToName;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TradingCurrency currencyFrom;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TradingCurrency currencyTo;

    @NotNull
    @Embedded
    private StatId.ProfitGroup profitGroup;
}
