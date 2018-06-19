package com.gtc.tradinggateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.tradinggateway.config.Const.CONF_ROOT_CHILD;
import static com.gtc.tradinggateway.config.Const.Clients.GDAX;

/**
 * Created by Valentyn Berezin on 16.01.18.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + GDAX)
public class GdaxConfig extends BaseConfig {

    public GdaxConfig(ConfigFactory factory) {
        mapper = factory.defaultMapper();
        restTemplate = factory.defaultRestTemplate(mapper);
    }
}
