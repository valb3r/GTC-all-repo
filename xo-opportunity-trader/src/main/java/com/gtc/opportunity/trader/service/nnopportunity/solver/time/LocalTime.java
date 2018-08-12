package com.gtc.opportunity.trader.service.nnopportunity.solver.time;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Primarily for test support.
 */
@Service
@RequiredArgsConstructor
public class LocalTime {

    public long timestampMs() {
        return System.currentTimeMillis();
    }
}
