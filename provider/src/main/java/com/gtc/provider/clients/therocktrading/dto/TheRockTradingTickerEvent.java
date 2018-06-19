package com.gtc.provider.clients.therocktrading.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 12.01.18.
 */
@Data
public class TheRockTradingTickerEvent {

    private String symbol;
    private double quantity;
    private double value;
}
