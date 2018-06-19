package com.gtc.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.provider.config.Const.CONF_ROOT_CHILD;
import static com.gtc.provider.config.Const.WEX;

/**
 * Created by mikro on 16.01.2018.
 */
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + WEX)
public class WexConf extends BaseClientConf {
}
