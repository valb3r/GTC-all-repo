package com.gtc.tradinggateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtc.tradinggateway.meta.PairSymbol;
import com.gtc.tradinggateway.meta.TradingCurrency;
import lombok.Data;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Valentyn Berezin on 16.01.18.
 */
@Data
public class BaseConfig {

    protected String wsBase;
    protected String restBase;
    protected String passphrase;
    protected String publicKey;
    protected String secretKey;

    protected ObjectMapper mapper = new ObjectMapper();

    protected RestTemplate restTemplate = new RestTemplate();

    protected Map<String, PairSymbol> pairs;

    protected Map<String, String> customResponseCurrencyMapping = new HashMap<>();

    protected Map<TradingCurrency, String> customCurrencyName;

    public void setCustomCurrencyName(List<String> list) {
        customCurrencyName = list.stream()
                .collect(
                        HashMap::new,
                        (HashMap<TradingCurrency, String> map, String val) -> {
                            String[] pair = val.split("=");
                            map.put(TradingCurrency.fromCode(pair[0]), pair[1]);
                        },
                        HashMap::putAll);
    }

    public void setCustomResponseCurrencyMapping(List<String> list) {
        list.forEach(it -> {
            String[] origMap = it.split("=");
            customResponseCurrencyMapping.put(origMap[0], TradingCurrency.fromCode(origMap[1]).getCode());
        });
    }

    public void setPairs(List<String> list) {
        pairs = parse(list);
    }

    public Optional<PairSymbol> pairFromCurrency(TradingCurrency from, TradingCurrency to) {
        String symbol = from.toString() + "-" + to.toString();
        PairSymbol pair = pairs.get(symbol);
        if (pair != null) {
            return Optional.of(pair);
        }
        String invertedSymbol = to.toString() + "-" + from.toString();
        PairSymbol invertedPair = pairs.get(invertedSymbol);
        return invertedPair != null ? Optional.of(invertedPair.invert()) : Optional.empty();
    }

    public PairSymbol pairFromCurrencyOrThrow(TradingCurrency from, TradingCurrency to) {

        return pairFromCurrency(from, to).orElseThrow(() -> new IllegalStateException(
                "Pair from " + from.toString() + " to " + to.toString() + " is not supported")
        );
    }

    private Map<String, PairSymbol> parse(List<String> input) {
        return input.stream()
                .collect(
                        HashMap::new,
                        (HashMap<String, PairSymbol> map, String val) -> {
                            String[] mapping = val.split("=", 2);
                            String[] symbol = mapping[0].split("-", 2);
                            map.computeIfAbsent(mapping[0], (String mKey) ->
                                    new PairSymbol(
                                            TradingCurrency.fromCode(symbol[0]),
                                            TradingCurrency.fromCode(symbol[1]), mapping[1]));
                        },
                        HashMap::putAll);
    }
}
