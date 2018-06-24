package com.gtc.tradinggateway.service.okex;

import com.gtc.model.gateway.data.OrderDto;
import com.gtc.tradinggateway.aspect.rate.IgnoreRateLimited;
import com.gtc.tradinggateway.aspect.rate.RateLimited;
import com.gtc.tradinggateway.config.OkexConfig;
import com.gtc.tradinggateway.meta.PairSymbol;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.Account;
import com.gtc.tradinggateway.service.CreateOrder;
import com.gtc.tradinggateway.service.ManageOrders;
import com.gtc.tradinggateway.service.Withdraw;
import com.gtc.tradinggateway.service.dto.OrderCreatedDto;
import com.gtc.tradinggateway.service.okex.dto.*;
import com.gtc.tradinggateway.util.CodeMapper;
import com.gtc.tradinggateway.util.DefaultInvertHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.*;

import static com.gtc.tradinggateway.config.Const.Clients.OKEX;

/**
 * Created by Valentyn Berezin on 23.06.18.
 * Note, that while okex has ws with capability to create orders, it does not assign user supplied id, so not usable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@RateLimited(ratePerMinute = "${app.okex.ratePerM}", minSeparationMs = "${app.okex.minRequestSeparationMs}", mode = RateLimited.Mode.METHOD)
public class OkexRestService implements CreateOrder, ManageOrders, Withdraw, Account {

    private static final String CREATE = "/trade.do";
    private static final String BALANCES = "/userinfo.do";
    private static final String ORDER = "/order_info.do";
    private static final String CANCEL = "/cancel_order.do";
    private static final String WITHDRAW ="/withdraw.do";

    private final OkexConfig cfg;

    @Override
    @RateLimited(ratePerMinute = "${app.okex.createRatePerM}", minSeparationMs = "${app.okex.minRequestSeparationMs}")
    public Optional<OrderCreatedDto> create(String tryToAssignId, TradingCurrency from, TradingCurrency to,
                                            BigDecimal amount, BigDecimal price) {

        PairSymbol pair = cfg.pairFromCurrencyOrThrow(from, to);

        BigDecimal calcAmount = DefaultInvertHandler.amountFromOrig(pair, amount, price);
        BigDecimal calcPrice = DefaultInvertHandler.priceFromOrig(pair, price);

        ResponseEntity<OkexCreateOrderResponse> response = cfg.getRestTemplate().exchange(
                cfg.getRestBase() + CREATE,
                HttpMethod.POST,
                new HttpEntity<>(new OkexCreateOrderRequest(
                        pair.getSymbol(),
                        DefaultInvertHandler.amountToBuyOrSell(calcAmount),
                        calcPrice, calcAmount.abs()
                ), headers()),
                OkexCreateOrderResponse.class
        );

        assertResponse(response.getBody());

        return Optional.of(response.getBody().mapToCreated(tryToAssignId, pair.getSymbol()));
    }

    @Override
    public Map<TradingCurrency, BigDecimal> balances() {
        ResponseEntity<OkexBalanceResponse> response = cfg.getRestTemplate().exchange(
                cfg.getRestBase() + BALANCES,
                HttpMethod.POST,
                new HttpEntity<>(new HashMap<>(), headers()),
                OkexBalanceResponse.class
        );

        assertResponse(response.getBody());

        Map<TradingCurrency, BigDecimal> results = new EnumMap<>(TradingCurrency.class);
        OkexBalanceResponse assets = response.getBody();
        for (Map.Entry<String, BigDecimal> asset : assets.getInfo().getFunds().getFree().entrySet()) {
            CodeMapper.mapAndPut(asset.getKey(), asset.getValue(), cfg, results);
        }
        return results;
    }

    @Override
    public Optional<OrderDto> get(String id) {
        String[] pairAndId = id.split("\\.");
        ResponseEntity<OkexGetOrderResponse> response = cfg.getRestTemplate().exchange(
                cfg.getRestBase() + ORDER,
                HttpMethod.POST,
                new HttpEntity<>(new OkexGetOrderRequest(pairAndId[1], pairAndId[0]), headers()),
                OkexGetOrderResponse.class
        );

        assertResponse(response.getBody());

        return response.getBody().mapTo();
    }

    @Override
    public List<OrderDto> getOpen(TradingCurrency from, TradingCurrency to) {
        ResponseEntity<OkexGetOrderResponse> response = cfg.getRestTemplate().exchange(
                cfg.getRestBase() + ORDER,
                HttpMethod.POST,
                new HttpEntity<>(new OkexGetOrderRequest(
                        "-1",
                        cfg.pairFromCurrencyOrThrow(from, to).getSymbol()), headers()
                ),
                OkexGetOrderResponse.class
        );

        assertResponse(response.getBody());

        return response.getBody().mapAll();
    }

    @Override
    public void cancel(String id) {
        String[] pairAndId = id.split("\\.");
        ResponseEntity<OkexCancelOrderResponse> response = cfg.getRestTemplate().exchange(
                cfg.getRestBase() + CANCEL,
                HttpMethod.POST,
                new HttpEntity<>(new OkexCancelOrderRequest(pairAndId[1], pairAndId[0]), headers()),
                OkexCancelOrderResponse.class
        );

        assertResponse(response.getBody());
    }

    // Most probably not working - needs fund password
    @Override
    public void withdraw(TradingCurrency currency, BigDecimal amount, String destination) {
        ResponseEntity<OkexWithdrawResponse> response = cfg.getRestTemplate().exchange(
                cfg.getRestBase() + WITHDRAW,
                HttpMethod.POST,
                new HttpEntity<>(new OkexWithdrawRequest(
                        currency.getCode().toLowerCase(), destination, amount), headers()
                ),
                OkexWithdrawResponse.class
        );

        assertResponse(response.getBody());
    }

    @Override
    @IgnoreRateLimited
    public String name() {
        return OKEX;
    }

    private MultiValueMap<String, String> headers() {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add(
                HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:53.0) Gecko/20100101 Firefox/53.0)"
        );

        return headers;
    }

    private void assertResponse(IsResultOk ok) {
        if (ok == null) {
            throw new IllegalStateException("Unparseable message");
        }

        if (!ok.isResult()) {
            throw new IllegalStateException(ErrorCodes.translate(ok.getErrorCode()));
        }
    }
}
