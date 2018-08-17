package com.gtc.tradinggateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.gtc.tradinggateway.BaseMockitoTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Valentyn Berezin on 15.08.18.
 */
class ConfigFactoryTest extends BaseMockitoTest {

    @InjectMocks
    private ConfigFactory factory;

    @Test
    @SneakyThrows
    void defaultMapperMapsBigDecimalAsPlain() {
        ObjectMapper mapper = factory.defaultMapper();
        Map<String, BigDecimal> test = ImmutableMap.of("v", new BigDecimal("1E+1"));

        String value = mapper.writer().writeValueAsString(test);

        assertThat(value).isEqualTo("{\"v\":10}");
    }
}
