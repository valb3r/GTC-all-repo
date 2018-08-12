package com.gtc.opportunity.trader.service.nnopportunity.solver.time;

import com.gtc.opportunity.trader.BaseMockitoTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Valentyn Berezin on 12.08.18.
 */
class RateLimiterTest extends BaseMockitoTest {

    private static final long INIT_MS = System.currentTimeMillis();
    private static final long WINDOW = 1000;

    @Test
    void tryAcquire() {
        LocalTime time = mock(LocalTime.class);
        when(time.timestampMs()).thenReturn(INIT_MS);
        RateLimiter limiter = new RateLimiter(WINDOW, time);

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
        assertThat(limiter.tryAcquire()).isFalse();

        when(time.timestampMs()).thenReturn(INIT_MS + WINDOW);
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
        assertThat(limiter.tryAcquire()).isFalse();
    }
}
