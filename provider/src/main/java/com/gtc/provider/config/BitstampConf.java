package com.gtc.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.provider.config.Const.BITSTAMP;
import static com.gtc.provider.config.Const.CONF_ROOT_CHILD;

/**
 * Created by Valentyn Berezin on 07.01.18.
 */
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + BITSTAMP)
public class BitstampConf extends BaseClientConf {
}
