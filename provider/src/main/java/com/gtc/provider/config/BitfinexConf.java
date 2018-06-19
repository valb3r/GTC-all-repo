package com.gtc.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.provider.config.Const.BITFINEX;
import static com.gtc.provider.config.Const.CONF_ROOT_CHILD;

/**
 * Created by Valentyn Berezin on 29.12.17.
 */
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + BITFINEX)
public class BitfinexConf extends BaseClientConf {
}
