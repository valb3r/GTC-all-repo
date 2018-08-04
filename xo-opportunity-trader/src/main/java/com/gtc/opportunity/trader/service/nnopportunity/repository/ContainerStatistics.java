package com.gtc.opportunity.trader.service.nnopportunity.repository;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 04.08.18.
 */
@Data
public class ContainerStatistics {

    private final double avgNoopAgeS;
    private final double avgActAgeS;

    private final double minCacheFullness;
    private final double minLabelFullness;
}
