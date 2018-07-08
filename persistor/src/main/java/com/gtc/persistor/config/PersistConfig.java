package com.gtc.persistor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.gtc.persistor.config.Const.Persist.PERSIST_CFG;

/**
 * Created by Valentyn Berezin on 07.07.18.
 */
@Data
@Configuration
@ConfigurationProperties(PERSIST_CFG)
public class PersistConfig {

    private String dir;
}
