package com.gtc.provider.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.provider.config.Const.CONF_ROOT_CHILD;
import static com.gtc.provider.config.Const.Write.WRITE;

/**
 * Created by Valentyn Berezin on 02.01.18.
 */
@Data
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + WRITE)
public class WriteConf {

    private Histogram histogram;

    @Data
    public static class Histogram {

        private int resolution;
        // how far to deviate from buy/sell inflection point (point when amount changes sign) in %
        private double deviateFromSignChangePct;
    }
}
