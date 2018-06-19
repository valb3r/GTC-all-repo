package com.gtc.provider.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static com.gtc.provider.config.Const.CLIENTS;
import static com.gtc.provider.config.Const.CONF_ROOT_CHILD;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + CLIENTS)
@Data
public class ClientsConf {

    List<String> active = new ArrayList<>();
}
