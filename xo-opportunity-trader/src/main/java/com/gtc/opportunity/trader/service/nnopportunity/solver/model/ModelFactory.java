package com.gtc.opportunity.trader.service.nnopportunity.solver.model;

import com.gtc.opportunity.trader.service.nnopportunity.dto.Snapshot;
import com.gtc.opportunity.trader.service.nnopportunity.solver.time.LocalTime;
import com.gtc.opportunity.trader.service.xoopportunity.creation.ConfigCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Service
@RequiredArgsConstructor
public class ModelFactory {

    private final LocalTime localTime;
    private final FeatureMapper mapper;
    private final ConfigCache cache;

    public NnModelPredict buildModel(Snapshot snapshot) throws NnModelPredict.TrainingFailed {
        return new NnModelPredict(localTime, cache.requireConfig(snapshot), snapshot, mapper);
    }
}
