package com.gtc.tradinggateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static com.gtc.tradinggateway.config.Const.CONF_ROOT_CHILD;
import static com.gtc.tradinggateway.config.Const.Clients.CLIENTS;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + CLIENTS)
@Data
public class ClientsConf {

    List<String> active = new ArrayList<>();
}
