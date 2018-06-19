package com.gtc.opportunity.trader.cqe.domain;

import com.gtc.meta.TradingCurrency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import javax.persistence.Embedded;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * You can sell on `from` (they will buy with {@link #marketFromBestBuyPrice}) and you can buy on `to`
 * (they will sell {@link #marketToBestSellPrice})
 */
@Data
@Builder
@ToString
@AllArgsConstructor
public class CrossMarketOpportunity {

    private String uuid;

    private String clientFrom;

    private String clientTo;

    private TradingCurrency currencyFrom;

    private TradingCurrency currencyTo;

    private LocalDateTime openedOn;

    private LocalDateTime updatedOn;

    private LocalDateTime closedOn;

    private boolean closed;

    private int eventCount;

    @Embedded
    private Statistic histWin;

    @Embedded
    private Statistic marketFromBestBuyPrice;

    @NotNull
    @Embedded
    private Statistic marketFromBestBuyAmount;

    @Embedded
    private Statistic marketToBestSellPrice;

    @Embedded
    private Statistic marketToBestSellAmount;
}
