package com.gtc.opportunity.trader.service.dto;

import lombok.Data;
import lombok.experimental.Delegate;

import java.util.List;

/**
 * Created by Valentyn Berezin on 27.08.18.
 */
@Data
public class FlatOrderBookWithHistory {

    @Delegate
    private final FlatOrderBook book;

    private final List<FlatOrderBook> history;
}
