package com.gtc.tradinggateway.service.therocktrading;

import com.gtc.model.gateway.data.OrderDto;
import com.gtc.tradinggateway.aspect.rate.IgnoreRateLimited;
import com.gtc.tradinggateway.aspect.rate.RateLimited;
import com.gtc.tradinggateway.config.TheRockTradingConfig;
import com.gtc.tradinggateway.meta.PairSymbol;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.Account;
import com.gtc.tradinggateway.service.CreateOrder;
import com.gtc.tradinggateway.service.ManageOrders;
import com.gtc.tradinggateway.service.Withdraw;
import com.gtc.tradinggateway.service.dto.OrderCreatedDto;
import com.gtc.tradinggateway.service.therocktrading.dto.*;
import com.gtc.tradinggateway.util.CodeMapper;
import com.gtc.tradinggateway.util.DefaultInvertHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.gtc.tradinggateway.config.Const.Clients.THEROCKTRADING;

@Slf4j
@Service
@RequiredArgsConstructor
@RateLimited(ratePerMinute = "${app.therocktrading.ratePerM}", mode = RateLimited.Mode.CLASS)
public class TheRockTradingRestService implements Account, CreateOrder, ManageOrders, Withdraw {

    private static final String BALANCE = "/v1/balances";
    private static final String FUNDS = "/v1/funds";
    private static final String ORDERS = "/orders";
    private static final String WITHDRAW = "/v1/atms/withdraw";

    private final TheRockTradingConfig cfg;
    private final TheRockTradingEncryptionService signer;

    @Override
    public Map<TradingCurrency, BigDecimal> balances() {
        String url = cfg.getRestBase() + BALANCE;
        ResponseEntity<TheRockTradingBalanceResponseDto> resp = cfg.getRestTemplate()
                .exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders(url)),
                        TheRockTradingBalanceResponseDto.class);
        Map<TradingCurrency, BigDecimal> results = new EnumMap<>(TradingCurrency.class);
        TheRockTradingBalanceResponseDto response = resp.getBody();
        List<TheRockTradingBalanceResponseDto.BalanceItem> assets = response.getBalances();
        for (TheRockTradingBalanceResponseDto.BalanceItem asset : assets) {
            CodeMapper.mapAndPut(asset.getCurrency(), asset.getBalance(), cfg, results);
        }
        return results;
    }

    @Override
    @RateLimited(ratePerMinute = "${app.therocktrading.createRatePerM}")
    public Optional<OrderCreatedDto> create(String tryToAssignId, TradingCurrency from, TradingCurrency to,
                                            BigDecimal amount, BigDecimal price) {
        PairSymbol pair = cfg.pairFromCurrencyOrThrow(from, to);

        BigDecimal calcAmount = DefaultInvertHandler.amountFromOrig(pair, amount, price);
        BigDecimal calcPrice = DefaultInvertHandler.priceFromOrig(pair, price);

        TheRockTradingCreateRequestDto requestDto = new TheRockTradingCreateRequestDto(
                pair.toString(),
                DefaultInvertHandler.amountToBuyOrSell(calcAmount),
                calcAmount.abs().toString(),
                calcPrice.toString()
        );

        String url = cfg.getRestBase() + FUNDS + "/" + pair.toString() + ORDERS;

        ResponseEntity<TheRockTradingCreateResponseDto> resp = cfg.getRestTemplate()
                .exchange(
                        url,
                        HttpMethod.POST,
                        new HttpEntity<>(requestDto, signer.restHeaders(url)),
                        TheRockTradingCreateResponseDto.class);

        return Optional.of(resp.getBody().getOrder().mapToCreate());
    }

    @Override
    public Optional<OrderDto> get(String id) {
        SymbolAndId symbolAndId = new SymbolAndId(id);

        String url = cfg.getRestBase() + FUNDS + "/" + symbolAndId.pair + ORDERS + "/" + symbolAndId.id;

        ResponseEntity<TheRockTradingOrderDto> resp = cfg.getRestTemplate()
                .exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders(url)),
                        TheRockTradingOrderDto.class);

        return Optional.of(resp.getBody().mapTo());
    }

    @Override
    public List<OrderDto> getOpen(TradingCurrency from, TradingCurrency to) {
        PairSymbol pair = cfg.pairFromCurrencyOrThrow(from, to);

        String url = cfg.getRestBase() + FUNDS + "/" + pair + ORDERS;

        ResponseEntity<TheRockTradingGetOpenResponseDto> resp = cfg.getRestTemplate()
                .exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders(url)),
                        TheRockTradingGetOpenResponseDto.class);

        return resp
                .getBody()
                .getOrders()
                .stream()
                .map(TheRockTradingOrderDto::mapTo)
                .collect(Collectors.toList());
    }

    @Override
    public void cancel(String id) {
        SymbolAndId symbolAndId = new SymbolAndId(id);

        String url = cfg.getRestBase() + FUNDS + "/" + symbolAndId.pair + ORDERS + "/" + symbolAndId.id;

        cfg.getRestTemplate()
                .exchange(
                        url,
                        HttpMethod.DELETE,
                        new HttpEntity<>(signer.restHeaders(url)),
                        Object.class);
    }

    @Override
    public void withdraw(TradingCurrency currency, BigDecimal amount, String destination) {
        String url = cfg.getRestBase() + WITHDRAW;

        TheRockTradingWithdrawRequestDto requestDto = new TheRockTradingWithdrawRequestDto(
                destination,
                currency.toString(),
                amount
        );

        cfg.getRestTemplate()
                .exchange(
                        url,
                        HttpMethod.POST,
                        new HttpEntity<>(requestDto, signer.restHeaders(url)),
                        Object.class);
    }

    @Override
    @IgnoreRateLimited
    public String name() {
        return THEROCKTRADING;
    }

    public static class SymbolAndId {
        private final String id;
        private final String pair;

        public SymbolAndId(String id) {
            String[] idPair = id.split("\\.");
            pair = idPair[0];
            this.id = idPair[1];
        }

        public SymbolAndId(String id, String pair) {
            this.pair = pair;
            this.id = id;
        }

        public String toString() {
            return pair + "." + id;
        }
    }
}
