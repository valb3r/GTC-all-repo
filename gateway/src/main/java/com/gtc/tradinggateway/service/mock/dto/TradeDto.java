package com.gtc.tradinggateway.service.mock.dto;

import com.gtc.tradinggateway.meta.TradingCurrency;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 08.03.18.
 */
@Data
public class TradeDto {

    private final String id;
    private final TradingPair pair;
    private final boolean isSell;
    private final BigDecimal amount;
    private final BigDecimal price;

    @Data
    public static class TradingPair {

        private final TradingCurrency from;
        private final TradingCurrency to;
    }
}
