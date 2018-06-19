package com.gtc.opportunity.trader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.opportunity.trader.config.Const.CONF_ROOT_CHILD;
import static com.gtc.opportunity.trader.config.Const.Cache.CACHE;

/**
 * Created by Valentyn Berezin on 26.02.18.
 */
@Data
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + CACHE)
public class CacheConfig {

    private Cfg cfgCache;
    private Wallet walletIds;

    @Data
    public static class Cfg {

        private int liveS = 60;
        private int size = 2048;
    }

    @Data
    public static class Wallet {

        private int liveS = 60;
        private int size = 2048;
    }
}
