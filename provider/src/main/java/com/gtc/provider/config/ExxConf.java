package com.gtc.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.provider.config.Const.CONF_ROOT_CHILD;
import static com.gtc.provider.config.Const.EXX;

/**
 * Created by mikro on 14.01.2018.
 */
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + EXX)
public class ExxConf extends BaseClientConf {
}
