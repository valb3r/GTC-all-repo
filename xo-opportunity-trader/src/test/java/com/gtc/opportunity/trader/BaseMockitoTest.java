package com.gtc.opportunity.trader;

import org.junit.After;
import org.junit.Before;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.validateMockitoUsage;

/**
 * Created by Valentyn Berezin on 10.03.18.
 */
public abstract class BaseMockitoTest {

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
