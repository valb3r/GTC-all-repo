package com.gtc.tradinggateway.service.binance;

import com.google.common.collect.ImmutableMap;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.tradinggateway.aspect.rate.IgnoreRateLimited;
import com.gtc.tradinggateway.aspect.rate.RateLimited;
import com.gtc.tradinggateway.config.BinanceConfig;
import com.gtc.tradinggateway.config.converters.FormHttpMessageToPojoConverter;
import com.gtc.tradinggateway.meta.PairSymbol;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.Account;
import com.gtc.tradinggateway.service.CreateOrder;
import com.gtc.tradinggateway.service.ManageOrders;
import com.gtc.tradinggateway.service.Withdraw;
import com.gtc.tradinggateway.service.binance.dto.*;
import com.gtc.tradinggateway.service.dto.OrderCreatedDto;
import com.gtc.tradinggateway.util.CodeMapper;
import com.gtc.tradinggateway.util.DefaultInvertHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

import static com.gtc.tradinggateway.config.Const.Clients.BINANCE;

/**
 * Done orders will appear in /order - no need for extra calls.
 * Validated basic functionality (create, get, get all, cancel)
 * 05.03.2018
 */
@Slf4j
@Service
@RequiredArgsConstructor
@RateLimited(ratePerMinute = "${app.binance.ratePerM}", mode = RateLimited.Mode.CLASS)
public class BinanceRestService implements ManageOrders, Withdraw, Account, CreateOrder {

    private static final String ORDERS = "/api/v3/order";
    private static final String ALL_ORDERS = "/api/v3/openOrders";
    private static final String BALANCES = "/api/v3/account";
    private static final String WITHDRAWAL = "/wapi/v3/withdraw.html";

    private final BinanceConfig cfg;
    private final BinanceEncryptionService signer;

    @Override
    public Optional<OrderDto> get(String id) {
        BinanceRequestOrderDto orderDto = new BinanceRequestOrderDto(id);
        ResponseEntity<BinanceGetOrderDto> resp = cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase() + ORDERS + getQueryString(orderDto),
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders()),
                        BinanceGetOrderDto.class);

        return Optional.of(resp.getBody().mapTo());
    }

    @SneakyThrows
    @IgnoreRateLimited
    public Map<String, String> getSignedBody(String toSign) {
        return ImmutableMap.of("signature", signer.generate(toSign));
    }

    @Override
    public List<OrderDto> getOpen(TradingCurrency from, TradingCurrency to) {
        BinanceRequestDto dto = new BinanceRequestDto();
        RestTemplate template = cfg.getRestTemplate();
        ResponseEntity<BinanceGetOrderDto[]> resp = template
                .exchange(
                        cfg.getRestBase() + ALL_ORDERS + getQueryString(dto),
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders()),
                        BinanceGetOrderDto[].class);
        BinanceGetOrderDto[] list = resp.getBody();
        List<OrderDto> result = new ArrayList<>();
        for (BinanceGetOrderDto respDto : list) {
            result.add(respDto.mapTo());
        }
        return result;
    }

    @Override
    public void cancel(String id) {
        BinanceRequestOrderDto orderDto = new BinanceRequestOrderDto(id);
        cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase() + ORDERS + getQueryString(orderDto),
                        HttpMethod.DELETE,
                        new HttpEntity<>(signer.restHeaders()), Object.class);
    }

    @Override
    public Map<TradingCurrency, BigDecimal> balances() {
        BinanceRequestDto requestDto = new BinanceRequestDto();
        RestTemplate template = cfg.getRestTemplate();
        ResponseEntity<BinanceBalanceDto> resp = template
                .exchange(
                        cfg.getRestBase() + BALANCES + getQueryString(requestDto),
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders()),
                        BinanceBalanceDto.class);
        Map<TradingCurrency, BigDecimal> results = new EnumMap<>(TradingCurrency.class);
        BinanceBalanceDto response = resp.getBody();
        BinanceBalanceDto.BinanceBalanceAsset[] assets = response.getBalances();
        for (BinanceBalanceDto.BinanceBalanceAsset asset : assets) {
            CodeMapper.mapAndPut(asset.getCode(), asset.getAmount(), cfg, results);
        }
        return results;
    }

    @Override
    public void withdraw(TradingCurrency currency, BigDecimal amount, String destination) {
        BinanceRequestDto requestDto = new BinanceWithdrawalRequestDto(currency.toString(), amount, destination);
        cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase() + WITHDRAWAL + getQueryString(requestDto),
                        HttpMethod.POST,
                        new HttpEntity<>(signer.restHeaders()), Object.class);
    }

    @Override
    @RateLimited(ratePerMinute = "${app.binance.createRatePerM}")
    public Optional<OrderCreatedDto> create(String tryToAssignId, TradingCurrency from, TradingCurrency to,
                                            BigDecimal amount, BigDecimal price) {
        PairSymbol pair = cfg.pairFromCurrencyOrThrow(from, to);
        BigDecimal calcAmount = DefaultInvertHandler.amountFromOrig(pair, amount, price);
        BigDecimal calcPrice = DefaultInvertHandler.priceFromOrig(pair, price);
        BinancePlaceOrderRequestDto requestDto = new BinancePlaceOrderRequestDto(
                pair.getSymbol(),
                DefaultInvertHandler.amountToBuyOrSellUpper(calcAmount),
                calcAmount.abs(),
                calcPrice
        );
        RestTemplate template = cfg.getRestTemplate();
        ResponseEntity<BinanceGetOrderDto> resp = template
                .exchange(
                        cfg.getRestBase() + ORDERS + getQueryString(requestDto),
                        HttpMethod.POST,
                        new HttpEntity<>(signer.restHeaders()),
                        BinanceGetOrderDto.class);
        BinanceGetOrderDto result = resp.getBody();

        return Optional.of(
                OrderCreatedDto.builder()
                        .assignedId(resp.getBody().getPair() + "." + result.getId())
                        .build()
        );
    }

    private String getQueryString(Object queryObj) {
        return "?" + FormHttpMessageToPojoConverter.pojoSerialize(cfg.getMapper(), queryObj, this::getSignedBody);
    }

    @Override
    @IgnoreRateLimited
    public String name() {
        return BINANCE;
    }
}
