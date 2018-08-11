package com.gtc.opportunity.trader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.validateMockitoUsage;

/**
 * Created by Valentyn Berezin on 10.03.18.
 */
public abstract class BaseMockitoTest {

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterEach
    public void validate() {
        validateMockitoUsage();
    }
}
