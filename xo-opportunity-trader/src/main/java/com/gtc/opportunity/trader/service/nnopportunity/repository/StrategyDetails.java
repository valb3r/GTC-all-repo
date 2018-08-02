package com.gtc.opportunity.trader.service.nnopportunity.repository;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 02.08.18.
 */
@Data
public class StrategyDetails {

    private final Strategy strategy;
    private final double confidence;
}
