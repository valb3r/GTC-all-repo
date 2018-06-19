package com.gtc.provider.clients.therocktrading.dto;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 12.01.18.
 */
@Data
public class TheRockTradingOrderEvent {

    private String side;
    private double price;
    private double amount;
}
