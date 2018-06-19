package com.gtc.tradinggateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.tradinggateway.config.Const.CONF_ROOT_CHILD;
import static com.gtc.tradinggateway.config.Const.Clients.BINANCE;

/**
 * Created by mikro on 23.01.2018.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + BINANCE)
public class BinanceConfig extends BaseConfig {

    public BinanceConfig(ConfigFactory factory) {
        mapper = factory.defaultMapper();
        restTemplate = factory.defaultRestTemplate(mapper);
    }
}
