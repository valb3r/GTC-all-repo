package com.gtc.model.provider;

import com.gtc.meta.TradingCurrency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Created by Valentyn Berezin on 25.08.18.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SubscribeMarketPricesDto extends ProviderSubsDto {

    private TradingCurrency from;
    private Set<TradingCurrency> to;
}
