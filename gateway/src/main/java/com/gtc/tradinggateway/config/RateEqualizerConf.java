package com.gtc.tradinggateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.tradinggateway.config.Const.CONF_ROOT_CHILD;
import static com.gtc.tradinggateway.config.Const.RateEqualizer.RATE_EQUALIZER;

/**
 * Created by Valentyn Berezin on 10.08.18.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + RATE_EQUALIZER)
public class RateEqualizerConf {

    private int queueCapacity;
}
