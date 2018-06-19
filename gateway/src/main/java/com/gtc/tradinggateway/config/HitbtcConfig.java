package com.gtc.tradinggateway.config;

import com.google.common.collect.ImmutableList;
import com.gtc.tradinggateway.config.converters.FormHttpMessageToPojoConverter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static com.gtc.tradinggateway.config.Const.CONF_ROOT_CHILD;
import static com.gtc.tradinggateway.config.Const.Clients.HITBTC;

/**
 * Created by mikro on 12.02.2018.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + HITBTC)
public class HitbtcConfig extends BaseConfig {

    public HitbtcConfig(ConfigFactory factory) {
        mapper = factory.defaultMapper();
        restTemplate = factory.defaultRestTemplate(mapper);
        restTemplate.setMessageConverters(ImmutableList.of(
                new FormHttpMessageToPojoConverter(mapper),
                new MappingJackson2HttpMessageConverter(mapper)
        ));
    }
}
