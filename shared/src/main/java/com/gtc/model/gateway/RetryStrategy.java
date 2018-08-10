package com.gtc.model.gateway;

import lombok.Data;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Valentyn Berezin on 04.08.18.
 */
@Data
public class RetryStrategy implements Serializable {

    public static final RetryStrategy BASIC_RETRY = new RetryStrategy(1000, 3, 5);
    public static final RetryStrategy SHORT_RETRY =
            new RetryStrategy(ThreadLocalRandom.current().nextInt(200, 1000), 1, 1);

    private final int baseDelayMs;
    private final double backOffMultiplier;
    private final int maxRetries;
}
