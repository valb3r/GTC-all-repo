package com.gtc.model.provider;

import com.gtc.meta.TradingCurrency;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * Created by Valentyn Berezin on 25.08.18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SubscribeMarketPricesDto extends ProviderSubsDto {

    private TradingCurrency from;
    private Set<TradingCurrency> to;

    public SubscribeMarketPricesDto(TradingCurrency from, Set<TradingCurrency> to) {
        super(Mode.MARKET_PRICE);
        this.from = from;
        this.to = to;
    }
}
