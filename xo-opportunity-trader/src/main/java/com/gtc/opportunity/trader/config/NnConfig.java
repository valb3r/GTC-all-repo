package com.gtc.opportunity.trader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

import static com.gtc.opportunity.trader.config.Const.CONF_ROOT_CHILD;
import static com.gtc.opportunity.trader.config.Const.NeuralNet.NN_CONF;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Data
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + NN_CONF)
public class NnConfig {

    private Set<String> exchanges;
    private int collectNlabeled;
    private float noopThreshold;
}
