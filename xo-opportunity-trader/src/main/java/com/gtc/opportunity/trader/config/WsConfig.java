package com.gtc.opportunity.trader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.opportunity.trader.config.Const.CONF_ROOT_CHILD;
import static com.gtc.opportunity.trader.config.Const.Ws.WS;

/**
 * Created by Valentyn Berezin on 18.06.18.
 */
@Data
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + WS)
public class WsConfig {

    private String market;
    private String gateway;
    private int disconnectIfInactiveS;
}
