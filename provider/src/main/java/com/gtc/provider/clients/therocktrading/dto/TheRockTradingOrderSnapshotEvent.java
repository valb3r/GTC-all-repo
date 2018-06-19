package com.gtc.provider.clients.therocktrading.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Valentyn Berezin on 12.01.18.
 */
@Data
public class TheRockTradingOrderSnapshotEvent {

    private List<TheRockTradingOrderEvent> asks = new ArrayList<>();
    private List<TheRockTradingOrderEvent> bids = new ArrayList<>();
}
