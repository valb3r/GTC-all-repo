package com.gtc.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.provider.config.Const.CONF_ROOT_CHILD;
import static com.gtc.provider.config.Const.HITBTC;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + HITBTC)
public class HitBtcConf extends BaseClientConf {
}
