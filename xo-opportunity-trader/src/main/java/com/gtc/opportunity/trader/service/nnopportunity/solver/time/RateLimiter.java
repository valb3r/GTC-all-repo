package com.gtc.opportunity.trader.service.nnopportunity.solver.time;

import lombok.RequiredArgsConstructor;

/**
 * Created by Valentyn Berezin on 12.08.18.
 */
@RequiredArgsConstructor
public class RateLimiter {

    private static final long UNINITIALIZED = -1;

    private long lastTimestampMs = UNINITIALIZED;
    private final Object lock = new Object();

    private final long requiredWindowMs;
    private final LocalTime time;

    public boolean tryAcquire() {
        synchronized (lock) {
            long timeStamp = time.timestampMs();
            if (lastTimestampMs + requiredWindowMs <= timeStamp || UNINITIALIZED == lastTimestampMs) {
                lastTimestampMs = timeStamp;
                return true;
            }

            return false;
        }
    }
}
