package com.gtc.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.provider.config.Const.CONF_ROOT_CHILD;
import static com.gtc.provider.config.Const.POLONIEX;

/**
 * Created by Valentyn Berezin on 30.12.17.
 */
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + POLONIEX)
public class PoloniexConf extends BaseClientConf {
}
