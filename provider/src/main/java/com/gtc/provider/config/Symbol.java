package com.gtc.provider.config;

import com.gtc.meta.CurrencyPair;
import com.gtc.meta.TradingCurrency;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Data
public class Symbol {

    private Map<String, CurrencyPair> bid;
    private Map<String, CurrencyPair> ticker;
    private List<String> other;

    // parses values tBTCUSD=BTC-USD,
    public void setBid(List<String> input) {
        bid = parse(input);
    }

    // parses values BTCUSD=BTC-USD,
    public void setTicker(List<String> input) {
        ticker = parse(input);
    }

    private Map<String, CurrencyPair> parse(List<String> input) {
        return input.stream()
                .collect(
                        HashMap::new,
                        (map, val) -> {
                            String[] pair = val.split("=");
                            map.computeIfAbsent(pair[0], mKey -> new CurrencyPair(
                                    TradingCurrency.fromCode(pair[1].split("-")[0]),
                                    TradingCurrency.fromCode(pair[1].split("-")[1]))
                            );
                        },
                        HashMap::putAll
                );
    }
}
