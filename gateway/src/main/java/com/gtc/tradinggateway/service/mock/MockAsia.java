package com.gtc.tradinggateway.service.mock;

import com.gtc.tradinggateway.aspect.rate.IgnoreRateLimited;
import com.gtc.tradinggateway.config.MockExchangeConfig;
import org.springframework.stereotype.Component;

/**
 * Created by Valentyn Berezin on 09.03.18.
 */
@Component
public class MockAsia extends BaseMockExchangeApi {

    public MockAsia(MockExchangeConfig cfg) {
        super(cfg);
    }

    @Override
    @IgnoreRateLimited
    public String name() {
        return "mock-asia";
    }
}
