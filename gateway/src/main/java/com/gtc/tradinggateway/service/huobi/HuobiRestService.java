package com.gtc.tradinggateway.service.huobi;

import com.google.common.base.Charsets;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.tradinggateway.aspect.rate.IgnoreRateLimited;
import com.gtc.tradinggateway.aspect.rate.RateLimited;
import com.gtc.tradinggateway.config.HuobiConfig;
import com.gtc.tradinggateway.config.converters.FormHttpMessageToPojoConverter;
import com.gtc.tradinggateway.meta.PairSymbol;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.Account;
import com.gtc.tradinggateway.service.CreateOrder;
import com.gtc.tradinggateway.service.ManageOrders;
import com.gtc.tradinggateway.service.Withdraw;
import com.gtc.tradinggateway.service.dto.OrderCreatedDto;
import com.gtc.tradinggateway.service.huobi.dto.*;
import com.gtc.tradinggateway.util.CodeMapper;
import com.gtc.tradinggateway.util.DefaultInvertHandler;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.gtc.tradinggateway.config.Const.Clients.HUOBI;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
@RateLimited(ratePerMinute = "${app.huobi.ratePerM}", minSeparationMs = "${app.huobi.minRequestSeparationMs}", mode = RateLimited.Mode.CLASS)
public class HuobiRestService implements ManageOrders, Withdraw, Account, CreateOrder {

    private static final String ORDERS = "/v1/order/orders";
    private static final String CREATE_ORDER = ORDERS + "/place";
    private static final String CANCEL_ORDER = "/submitcancel";
    private static final String WITHDRAWAL = "/v1/dw/withdraw/api/create";
    private static final String ACCOUNTS = "/v1/account/accounts/";
    private static final String BALANCE = "/balance";

    private final HuobiConfig cfg;
    private final HuobiEncryptionService signer;

    private final AtomicReference<Long> accountId = new AtomicReference<>();

    private long accountId() {
        if (null != accountId.get()) {
            return accountId.get();
        }

        HuobiRequestDto requestDto = new HuobiRequestDto(cfg.getPublicKey());
        RestTemplate template = cfg.getRestTemplate();
        ResponseEntity<HuobiAccountsResponseDto> resp = template
                .exchange(
                        getQueryUri(HttpMethod.GET, ACCOUNTS, requestDto),
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders()),
                        HuobiAccountsResponseDto.class);
        accountId.set(resp.getBody().getPrimaryAccountOrThrow().getId());
        return accountId.get();
    }

    @Override
    @RateLimited(ratePerMinute = "${app.huobi.createRatePerM}")
    public Optional<OrderCreatedDto> create(String tryToAssignId, TradingCurrency from, TradingCurrency to,
                                            BigDecimal amount, BigDecimal price) {
        PairSymbol pair = cfg.pairFromCurrencyOrThrow(from, to);
        BigDecimal calcAmount = DefaultInvertHandler.amountFromOrig(pair, amount, price);
        BigDecimal calcPrice = DefaultInvertHandler.priceFromOrig(pair, price);
        HuobiCreateRequestDto dto = new HuobiCreateRequestDto(accountId(),
                DefaultInvertHandler.amountToBuyOrSell(calcAmount) + "-limit",
                calcAmount.abs(),
                calcPrice,
                pair.toString());
        HuobiRequestDto requestDto = new HuobiRequestDto(cfg.getPublicKey());
        RestTemplate template = cfg.getRestTemplate();
        ResponseEntity<HuobiCreateResponseDto> resp = template
                .exchange(
                        getQueryUri(HttpMethod.POST, CREATE_ORDER, requestDto),
                        HttpMethod.POST,
                        new HttpEntity<>(dto, signer.restHeaders(HttpMethod.POST)),
                        HuobiCreateResponseDto.class
                );

        resp.getBody().selfAssert();

        return Optional.of(
                OrderCreatedDto.builder()
                        .assignedId(SymbolAndId.valueOf(dto, resp.getBody()).toString())
                        .build());
    }

    @Override
    public Optional<OrderDto> get(String symbolId) {
        long id = SymbolAndId.valueOf(symbolId).getId();
        HuobiGetOrderRequestDto requestDto = new HuobiGetOrderRequestDto(cfg.getPublicKey(), id);
        RestTemplate template = cfg.getRestTemplate();
        ResponseEntity<HuobiGetResponseDto> resp = template
                .exchange(
                        getQueryUri(HttpMethod.GET, ORDERS + "/" + id, requestDto),
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders()),
                        HuobiGetResponseDto.class);
        return Optional.of(resp.getBody()
                .getOrder()
                .mapTo());
    }

    @Override
    @SneakyThrows
    public List<OrderDto> getOpen(TradingCurrency from, TradingCurrency to) {
        PairSymbol pair = cfg.pairFromCurrencyOrThrow(from, to);
        HuobiGetOrderListRequestDto requestDto = new HuobiGetOrderListRequestDto(cfg.getPublicKey(), pair.getSymbol());
        RestTemplate template = cfg.getRestTemplate();
        ResponseEntity<HuobiGetListResponseDto> resp = template
                .exchange(
                        getQueryUri(HttpMethod.GET, ORDERS, requestDto),
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders()),
                        HuobiGetListResponseDto.class);
        return resp.getBody().getOrders().stream().map(HuobiOrderDto::mapTo).collect(Collectors.toList());
    }

    @Override
    public void cancel(String id) {
        HuobiRequestDto requestDto = new HuobiRequestDto(cfg.getPublicKey());
        RestTemplate template = cfg.getRestTemplate();
        template.exchange(
                getQueryUri(HttpMethod.POST, ORDERS + "/" + SymbolAndId.valueOf(id).getId() + CANCEL_ORDER,
                        requestDto),
                HttpMethod.POST,
                new HttpEntity<>(signer.restHeaders(HttpMethod.POST)),
                Object.class);
    }

    @Override
    public Map<TradingCurrency, BigDecimal> balances() {
        HuobiRequestDto requestDto = new HuobiRequestDto(cfg.getPublicKey());
        RestTemplate template = cfg.getRestTemplate();
        ResponseEntity<HuobiBalanceResponseDto> resp = template
                .exchange(
                        getQueryUri(HttpMethod.GET, ACCOUNTS + accountId() + BALANCE, requestDto),
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders()),
                        HuobiBalanceResponseDto.class);
        Map<TradingCurrency, BigDecimal> results = new EnumMap<>(TradingCurrency.class);
        List<HuobiBalanceResponseDto.BalanceItem> assets = resp.getBody().getData().getList();
        for (HuobiBalanceResponseDto.BalanceItem item : assets) {
            if (!item.isTrade()) {
                continue;
            }

            CodeMapper.mapAndPut(item.getCurrency().toUpperCase(), item.getAmount(), cfg, results);
        }

        return results;
    }

    @Override
    public void withdraw(TradingCurrency currency, BigDecimal amount, String destination) {
        HuobiRequestDto dto = new HuobiRequestDto(cfg.getPublicKey());
        HuobiWithdrawalRequestDto requestDto = new HuobiWithdrawalRequestDto(
                destination,
                amount.toString(),
                currency.toString().toLowerCase());
        RestTemplate template = cfg.getRestTemplate();
        template.exchange(
                getQueryUri(HttpMethod.POST, WITHDRAWAL, dto),
                HttpMethod.POST,
                new HttpEntity<>(requestDto, signer.restHeaders(HttpMethod.POST)),
                Object.class);
    }

    @Override
    @IgnoreRateLimited
    public String name() {
        return HUOBI;
    }

    @SneakyThrows
    private URI getQueryUri(HttpMethod method, String path, Object queryObj) {
        // HUOBI USES FORM ENCODING IN QUERY !
        String query = FormHttpMessageToPojoConverter.pojoSerialize(cfg.getMapper(), queryObj, null);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(cfg.getRestBase())
                .path(path);
        Arrays.stream(query.split("&")).map(it -> it.split("=")).forEach(it -> builder.queryParam(it[0], it[1]));
        builder.queryParam(
                "Signature",
                URLEncoder.encode(signer.generate(method, path, query), Charsets.UTF_8.name())
        );
        return builder.build(true).toUri();
    }

    @Data
    private static class SymbolAndId {

        private final String symbol;
        private final long id;

        static SymbolAndId valueOf(HuobiCreateRequestDto request, HuobiCreateResponseDto resp) {
            return new SymbolAndId(request.getSymbol(), Long.valueOf(resp.getOrderId()));
        }

        static SymbolAndId valueOf(String combined) {
            String[] symbolId = combined.split("\\.");
            return new SymbolAndId(symbolId[0], Long.valueOf(symbolId[1]));
        }

        @Override
        public String toString() {
            return symbol + "." + id;
        }
    }
}
