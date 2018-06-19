package com.gtc.tradinggateway.service.mock;

import com.gtc.model.gateway.data.OrderDto;
import com.gtc.tradinggateway.aspect.rate.RateLimited;
import com.gtc.tradinggateway.config.MockExchangeConfig;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.Account;
import com.gtc.tradinggateway.service.CreateOrder;
import com.gtc.tradinggateway.service.ManageOrders;
import com.gtc.tradinggateway.service.Withdraw;
import com.gtc.tradinggateway.service.dto.OrderCreatedDto;
import com.gtc.tradinggateway.service.mock.dto.DepositDto;
import com.gtc.tradinggateway.service.mock.dto.Order;
import com.gtc.tradinggateway.service.mock.dto.TradeDto;
import com.gtc.tradinggateway.util.CodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 09.03.18.
 */
@Slf4j
@RequiredArgsConstructor
@RateLimited(ratePerMinute = "${app.mock.ratePerM}", mode = RateLimited.Mode.CLASS)
public abstract class BaseMockExchangeApi implements ManageOrders, Withdraw, Account, CreateOrder {

    private static final String ACCOUNT = "account";
    private static final String TRADE = "trade";
    private static final String ORDERS = "orders";

    private final MockExchangeConfig cfg;

    @Override
    public Map<TradingCurrency, BigDecimal> balances() {

        Map<String, BigDecimal> balances = cfg.getRestTemplate().exchange(
                new RequestEntity<>(
                        HttpMethod.GET,
                        UriComponentsBuilder
                        .fromHttpUrl(cfg.getRestBase())
                        .pathSegment(ACCOUNT, name(), cfg.getPublicKey())
                        .build().toUri()),
                new ParameterizedTypeReference<HashMap<String, BigDecimal>>(){}).getBody();

        Map<TradingCurrency, BigDecimal> results = new EnumMap<>(TradingCurrency.class);
        if (null != balances) {
            balances.forEach((key, value) -> CodeMapper.mapAndPut(key, value, cfg, results));
        }

        return results;
    }

    @Override
    @RateLimited(ratePerMinute = "${app.mock.createRatePerM}")
    public Optional<OrderCreatedDto> create(String tryToAssignId, TradingCurrency from, TradingCurrency to, BigDecimal amount, BigDecimal price) {
        URI target = UriComponentsBuilder
                .fromHttpUrl(cfg.getRestBase())
                .pathSegment(TRADE, name(), cfg.getPublicKey())
                .build().toUri();
        Order order = cfg.getRestTemplate().exchange(
                new RequestEntity<>(
                new TradeDto(tryToAssignId,
                        new TradeDto.TradingPair(from, to),
                        amount.compareTo(BigDecimal.ZERO) < 0,
                        amount.abs(),
                        price), HttpMethod.PUT, target), Order.class
        ).getBody();

        return Optional.of(
                OrderCreatedDto.builder()
                        .requestedId(tryToAssignId)
                        .assignedId(order.getId())
                        .isExecuted(BigDecimal.ZERO.equals(order.getAmount()))
                .build()
        );
    }

    @Override
    public Optional<OrderDto> get(String id) {
        URI target = UriComponentsBuilder
                .fromHttpUrl(cfg.getRestBase())
                .pathSegment(TRADE, name(), cfg.getPublicKey(), ORDERS, id)
                .build().toUri();
        Order order = cfg.getRestTemplate().getForObject(target, Order.class);

        return Optional.of(order.mapTo());
    }

    @Override
    public List<OrderDto> getOpen(TradingCurrency from, TradingCurrency to) {
        URI target = UriComponentsBuilder
                .fromHttpUrl(cfg.getRestBase())
                .pathSegment(TRADE, name(), cfg.getPublicKey(), ORDERS)
                .build().toUri();
        List<Order> orders =  cfg.getRestTemplate().exchange(
                new RequestEntity<>(HttpMethod.GET, target),
                new ParameterizedTypeReference<List<Order>>(){}).getBody();

        return orders.stream().map(Order::mapTo).collect(Collectors.toList());
    }

    @Override
    public void cancel(String id) {
        URI target = UriComponentsBuilder
                .fromHttpUrl(cfg.getRestBase())
                .pathSegment(TRADE, name(), cfg.getPublicKey(), id)
                .build().toUri();
        cfg.getRestTemplate().exchange(
                new RequestEntity<>(HttpMethod.DELETE, target), Order.class
        );
    }

    @Override
    public void withdraw(TradingCurrency currency, BigDecimal amount, String destination) {
        URI target = UriComponentsBuilder
                .fromHttpUrl(cfg.getRestBase())
                .pathSegment(ACCOUNT, name(), cfg.getPublicKey(), "withdraw")
                .build().toUri();
        cfg.getRestTemplate().put(target, new DepositDto(currency, amount));
    }
}
