package com.gtc.opportunity.trader.config;

import com.gtc.meta.CurrencyPair;
import com.gtc.meta.TradingCurrency;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

import static com.gtc.opportunity.trader.config.Const.CONF_ROOT_CHILD;
import static com.gtc.opportunity.trader.config.Const.NeuralNet.NN_CONF;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Data
@Configuration
@ConfigurationProperties(CONF_ROOT_CHILD + NN_CONF)
public class NnConfig {

    private Map<String, Set<CurrencyPair>> enabledOn;
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

    public void setEnabledOn(List<String> input) {
        enabledOn = parse(input);
    }

    private Map<String, Set<CurrencyPair>> parse(List<String> input) {
        return input.stream()
                .collect(
                        HashMap::new,
                        (HashMap<String, Set<CurrencyPair>> map, String val) -> {
                            String[] exchange = val.split("=", 2);
                            String[] symbol = exchange[1].split("-", 2);
                            map.computeIfAbsent(exchange[0], (String mKey) -> new HashSet<>())
                                    .add(new CurrencyPair(
                                            TradingCurrency.fromCode(symbol[0]),
                                            TradingCurrency.fromCode(symbol[1]))
                                    );
                        },
                        HashMap::putAll);
    }
}
