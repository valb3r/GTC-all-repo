package com.gtc.tradinggateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.tradinggateway.config.Const.CONF_ROOT_CHILD;
import static com.gtc.tradinggateway.config.Const.Clients.THEROCKTRADING;

@Getter
@Setter
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + THEROCKTRADING)
public class TheRockTradingConfig extends BaseConfig {

    public TheRockTradingConfig(ConfigFactory factory) {
        mapper = factory.defaultMapper();
        restTemplate = factory.defaultRestTemplate(mapper);
    }
}
