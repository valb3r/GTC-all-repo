package com.gtc.tradinggateway;

import org.junit.After;
import org.junit.Before;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.validateMockitoUsage;

/**
 * Created by Valentyn Berezin on 03.02.18.
 */
public class BaseMockitoTest {

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}

