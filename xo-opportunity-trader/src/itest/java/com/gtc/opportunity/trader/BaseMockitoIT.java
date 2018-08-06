package com.gtc.opportunity.trader;

import org.junit.After;
import org.junit.Before;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.validateMockitoUsage;

/**
 * Created by Valentyn Berezin on 02.08.18.
 */
public abstract class BaseMockitoIT {

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
