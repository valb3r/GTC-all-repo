package com.gtc.provider.market;

import com.gtc.meta.TradingCurrency;
import com.gtc.model.provider.MarketPrice;
import com.gtc.provider.service.SubsRegistry;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 25.08.18.
 */
@Service
@RequiredArgsConstructor
public class MarketPriceService {

    private final RestTemplate restTemplate = new RestTemplate();

    private final SubsRegistry subsRegistry;
    private final MarketSubsRegistry marketRegistry;

    @Value("${app.marketStat.cryptocompare}")
    private String apiUrl;

    @Trace(dispatcher = true)
    @Scheduled(fixedDelayString = "#{${app.marketStat.pollingDelayS} * 1000}")
    public void pollAndPublish() {
        marketRegistry.dataToPoll().forEach(this::getPriceAndPublish);
    }

    private void getPriceAndPublish(TradingCurrency from, Set<TradingCurrency> to) {
        Map<String, BigDecimal> prices = restTemplate.exchange(
                new RequestEntity<>(HttpMethod.GET, url(from, to)),
                new ParameterizedTypeReference<Map<String, BigDecimal>>() {})
                .getBody();

        if (null == prices) {
            return;
        }

        prices.forEach((code, price) ->
                subsRegistry.publishMarketPrice(new MarketPrice(from, TradingCurrency.fromCode(code), price))
        );
    }

    @SneakyThrows
    private URI url(TradingCurrency from, Set<TradingCurrency> to) {
        return new URI(
                apiUrl.replace("%FROM%", from.getCode())
                .replace("%TO%", to.stream().map(TradingCurrency::getCode).collect(Collectors.joining(",")))
        );
    }
}
