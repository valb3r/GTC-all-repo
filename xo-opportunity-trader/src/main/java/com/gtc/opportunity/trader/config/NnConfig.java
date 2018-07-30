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
    private int futureNwindow;
    private int collectNlabeled;
    private float noopThreshold;
    private float truthThreshold;
    private float proceedFalsePositive;
    private double averageDtSBetweenLabels;

    private double booksPerS;
    private int oldThresholdM;
    private double trainRelativeSize;

    private int layers;
    private int layerDim;
    private int iterations;
    private double learningRate;
    private double momentum;
    private double l2;
    private double futurePriceGainPct;
}
