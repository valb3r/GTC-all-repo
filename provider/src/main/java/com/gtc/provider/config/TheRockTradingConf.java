package com.gtc.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.provider.config.Const.CONF_ROOT_CHILD;
import static com.gtc.provider.config.Const.THE_ROCK_TRADING;

/**
 * Created by Valentyn Berezin on 12.01.18.
 */
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + THE_ROCK_TRADING)
public class TheRockTradingConf extends BaseClientConf {
}
