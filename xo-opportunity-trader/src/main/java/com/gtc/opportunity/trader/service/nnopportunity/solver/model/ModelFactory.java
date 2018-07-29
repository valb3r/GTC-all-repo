package com.gtc.opportunity.trader.service.nnopportunity.solver.model;

import com.gtc.opportunity.trader.config.NnConfig;
import com.gtc.opportunity.trader.service.nnopportunity.dto.Snapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Service
@RequiredArgsConstructor
public class ModelFactory {

    private final NnConfig nnConfig;
    private final FeatureMapper mapper;

    public NnModelPredict buildModel(Snapshot snapshot) throws NnModelPredict.TrainingFailed {
        return new NnModelPredict(nnConfig, snapshot, mapper);
    }
}
