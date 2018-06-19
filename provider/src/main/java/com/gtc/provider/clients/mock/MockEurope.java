package com.gtc.provider.clients.mock;

import com.gtc.provider.config.MockExchngConf;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.gtc.provider.clients.mock.Const.MOCK_ENABLED;

/**
 * Created by Valentyn Berezin on 09.03.18.
 */
@Component
@ConditionalOnProperty(name = MOCK_ENABLED, havingValue = "true")
public class MockEurope extends BaseMockClient {

    public MockEurope(MockExchngConf conf) {
        super(conf, "mock-europe");
    }
}
