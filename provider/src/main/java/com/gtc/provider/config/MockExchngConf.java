package com.gtc.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.provider.config.Const.CONF_ROOT_CHILD;
import static com.gtc.provider.config.Const.MOCK;

/**
 * Created by Valentyn Berezin on 09.03.18.
 */
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + MOCK)
public class MockExchngConf extends BaseClientConf {
}
