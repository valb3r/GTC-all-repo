package com.gtc.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.provider.config.Const.BINANCE;
import static com.gtc.provider.config.Const.CONF_ROOT_CHILD;

/**
 * Created by mikro on 07.01.2018.
 */
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + BINANCE)
public class BinanceConf extends BaseClientConf {
}
