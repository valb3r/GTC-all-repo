package com.gtc.model.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Valentyn Berezin on 01.01.18.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedOrder {

    private double minPrice;

    private double maxPrice;

    private double amount;

    private int bidCount;

    private boolean isSell;

    private short posId;

    private long latestBid;

    private long averageBid;

    private long oldestBid;
}
