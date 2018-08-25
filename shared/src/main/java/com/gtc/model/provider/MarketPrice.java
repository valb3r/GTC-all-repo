package com.gtc.model.provider;

import com.gtc.meta.TradingCurrency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 25.08.18.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketPrice {

    private final String meta = "META";

    private TradingCurrency from;
    private TradingCurrency to;
    private BigDecimal price;
}
