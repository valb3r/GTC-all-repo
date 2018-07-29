package com.gtc.opportunity.trader.service.nnopportunity.creation;

import com.gtc.opportunity.trader.config.NnConfig;
import com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Service
@RequiredArgsConstructor
public class CreateTradesService {

    private final NnConfig nnConfig;

    public void create(Strategy strategy) {

    }
}
