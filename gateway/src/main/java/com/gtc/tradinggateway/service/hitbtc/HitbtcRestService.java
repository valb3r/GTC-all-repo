package com.gtc.tradinggateway.service.hitbtc;

import com.gtc.model.gateway.data.OrderDto;
import com.gtc.tradinggateway.aspect.rate.IgnoreRateLimited;
import com.gtc.tradinggateway.aspect.rate.RateLimited;
import com.gtc.tradinggateway.config.HitbtcConfig;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.Account;
import com.gtc.tradinggateway.service.ManageOrders;
import com.gtc.tradinggateway.service.Withdraw;
import com.gtc.tradinggateway.service.hitbtc.dto.HitbtcBalanceItemDto;
import com.gtc.tradinggateway.service.hitbtc.dto.HitbtcOrderGetDto;
import com.gtc.tradinggateway.service.hitbtc.dto.HitbtcWithdrawRequestDto;
import com.gtc.tradinggateway.util.CodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.gtc.tradinggateway.config.Const.Clients.HITBTC;

/**
 * Done orders will appear in /trades.
 * Validated basic functionality (get, get all, cancel)
 * 05.03.2018
 */
@Slf4j
@Service
@RequiredArgsConstructor
@RateLimited(ratePerMinute = "${app.hitbtc.ratePerM}", minSeparationMs = "${app.hitbtc.minRequestSeparationMs}", mode = RateLimited.Mode.CLASS)
public class HitbtcRestService implements ManageOrders, Withdraw, Account {

    private static final String ORDERS = "/order/";
    private static final String HISTORY_ORDERS = "history/order";
    private static final String BALANCES = "/trading/balance";
    private static final String WITHDRAWAL = "/account/crypto/withdraw";

    private static final String PARAM_ID = "clientOrderId";

    private final HitbtcConfig cfg;
    private final HitbtcEncryptionService signer;

    @Override
    public Optional<OrderDto> get(String id) {
        try {
            return getOpenOrderById(id);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() != HttpStatus.BAD_REQUEST) {
                throw ex;
            }
            return getClosedOrderById(id);
        }
    }

    @Override
    public List<OrderDto> getOpen(TradingCurrency from, TradingCurrency to) {
        ResponseEntity<HitbtcOrderGetDto[]> resp = cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase() + ORDERS,
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders()),
                        HitbtcOrderGetDto[].class);
        return Arrays.stream(resp.getBody()).map(HitbtcOrderGetDto::mapTo).collect(Collectors.toList());

    }

    @Override
    public void cancel(String id) {
        cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase() + ORDERS + id,
                        HttpMethod.DELETE,
                        new HttpEntity<>(signer.restHeaders()), Object.class);
    }

    @Override
    public Map<TradingCurrency, BigDecimal> balances() {
        ResponseEntity<HitbtcBalanceItemDto[]> resp = cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase() + BALANCES,
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders()),
                        HitbtcBalanceItemDto[].class);
        Map<TradingCurrency, BigDecimal> results = new EnumMap<>(TradingCurrency.class);
        HitbtcBalanceItemDto[] assets = resp.getBody();
        for (HitbtcBalanceItemDto asset : assets) {
            CodeMapper.mapAndPut(asset.getCurrency(), asset.getAvailable(), cfg, results);
        }
        return results;
    }

    @SneakyThrows
    @Override
    public void withdraw(TradingCurrency currency, BigDecimal amount, String destination) {
        HitbtcWithdrawRequestDto requestDto = new HitbtcWithdrawRequestDto(destination, amount, currency.toString());

        cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase() + WITHDRAWAL,
                        HttpMethod.POST,
                        new HttpEntity<>(requestDto, signer.restHeaders()), Object.class);
    }

    @Override
    @IgnoreRateLimited
    public String name() {
        return HITBTC;
    }

    private Optional<OrderDto> getOpenOrderById(String id) {
        ResponseEntity<HitbtcOrderGetDto> resp = cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase() + ORDERS + id,
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders()),
                        HitbtcOrderGetDto.class);
        return Optional.of(resp.getBody().mapTo());
    }

    private Optional<OrderDto> getClosedOrderById(String id) {
        ResponseEntity<HitbtcOrderGetDto[]> resp = cfg.getRestTemplate()
                .exchange(
                        UriComponentsBuilder
                                .fromHttpUrl(cfg.getRestBase())
                                .pathSegment(HISTORY_ORDERS)
                                .queryParam(PARAM_ID, id)
                        .build().toUri(),
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders()),
                        HitbtcOrderGetDto[].class);
        if (resp.getBody().length == 0) {
            return Optional.empty();
        }

        return Optional.of(resp.getBody()[0].mapTo());
    }
}
