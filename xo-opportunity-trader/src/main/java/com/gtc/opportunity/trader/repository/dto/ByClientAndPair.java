package com.gtc.opportunity.trader.repository.dto;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.Client;
import lombok.Data;

/**
 * Created by Valentyn Berezin on 14.04.18.
 */
@Data
public class ByClientAndPair {

    private final Client client;
    private final TradingCurrency from;
    private final TradingCurrency to;
}
