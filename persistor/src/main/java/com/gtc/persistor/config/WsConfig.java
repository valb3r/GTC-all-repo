package com.gtc.persistor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static com.gtc.persistor.config.Const.Ws.CHILD;
import static com.gtc.persistor.config.Const.Ws.PROVIDER;

/**
 * Created by Valentyn Berezin on 01.07.18.
 */
@Data
@Configuration
@ConfigurationProperties(CHILD + PROVIDER)
public class WsConfig {

    private String provider;
    private List<String> marketsToSubscribe;
    private int disconnectIfInactiveS;
}
