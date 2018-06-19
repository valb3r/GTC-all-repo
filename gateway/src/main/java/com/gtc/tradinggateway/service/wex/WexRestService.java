package com.gtc.tradinggateway.service.wex;

import com.google.common.annotations.VisibleForTesting;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.tradinggateway.aspect.rate.IgnoreRateLimited;
import com.gtc.tradinggateway.aspect.rate.RateLimited;
import com.gtc.tradinggateway.aspect.synchroniz.Asynchronous;
import com.gtc.tradinggateway.aspect.synchroniz.LockAndProceed;
import com.gtc.tradinggateway.aspect.synchroniz.Synchronous;
import com.gtc.tradinggateway.config.WexConfig;
import com.gtc.tradinggateway.meta.PairSymbol;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.Account;
import com.gtc.tradinggateway.service.CreateOrder;
import com.gtc.tradinggateway.service.ManageOrders;
import com.gtc.tradinggateway.service.Withdraw;
import com.gtc.tradinggateway.service.dto.OrderCreatedDto;
import com.gtc.tradinggateway.service.wex.dto.*;
import com.gtc.tradinggateway.util.CodeMapper;
import com.gtc.tradinggateway.util.DefaultInvertHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static com.gtc.tradinggateway.config.Const.Clients.WEX;

/**
 * All functions except create (which simply jumps in priority) are synchronized due to nonce requirement.
 * Done orders will appear in /OrderInfo (no need for extra check).
 * Validated basic functionality (create, get, get all, cancel)
 * 05.03.2018
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Synchronous
@RateLimited(ratePerMinute = "${app.wex.ratePerM}", mode = RateLimited.Mode.CLASS)
public class WexRestService implements ManageOrders, Withdraw, Account, CreateOrder {

    private static final AtomicLong NONCE
            = new AtomicLong(LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond());
    private static final int PRIO_STEP = 25;

    private static final String BALANCES = "getInfo";
    private static final String CREATE = "Trade";
    private static final String GET = "OrderInfo";
    private static final String GET_OPEN = "ActiveOrders";
    private static final String CANCEL = "CancelOrder";
    private static final String WITHDRAW = "WithdrawCoin";

    private final WexConfig cfg;
    private final WexEncryptionService signer;

    @Override
    @SneakyThrows
    public Map<TradingCurrency, BigDecimal> balances() {
        BaseWexRequest request = new BaseWexRequest(nonce(), BALANCES);
        ResponseEntity<WexBalancesDto> resp = cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase(),
                        HttpMethod.POST,
                        new HttpEntity<>(request, signer.sign(request)),
                        WexBalancesDto.class);

        checkResponse(resp.getBody());

        Map<TradingCurrency, BigDecimal> results = new EnumMap<>(TradingCurrency.class);
        WexBalancesDto.Value value = resp.getBody().getRet();
        value.getFunds().forEach((key, amount) ->
                CodeMapper.mapAndPut(key, amount, cfg, results)
        );

        return results;
    }

    /**
     * Uses priority mechanism instead of synchronized - invalidates #PRIO_STEP requests before it and jumps ahead.
     * This way we can execute it immediately out of sync. Hopefully 2 create requests will not collide.
     */
    @Override
    @LockAndProceed
    @RateLimited(ratePerMinute = "${app.wex.createRatePerM}")
    public Optional<OrderCreatedDto> create(
            String tryToAssignId, TradingCurrency from, TradingCurrency to, BigDecimal amount, BigDecimal price) {
        PairSymbol pair = cfg.pairFromCurrencyOrThrow(from, to);
        BigDecimal calcAmount = DefaultInvertHandler.amountFromOrig(pair, amount, price);
        BigDecimal calcPrice = DefaultInvertHandler.priceFromOrig(pair, price);
        WexCreateOrder request = new WexCreateOrder(nonce(PRIO_STEP), CREATE, pair.getSymbol(),
                DefaultInvertHandler.amountToBuyOrSell(calcAmount),
                calcPrice,
                calcAmount.abs()
        );

        ResponseEntity<WexCreateResponse> resp = cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase(),
                        HttpMethod.POST,
                        new HttpEntity<>(request, signer.sign(request)),
                        WexCreateResponse.class);

        checkResponse(resp.getBody());

        return Optional.of(
                OrderCreatedDto.builder()
                        .assignedId(String.valueOf(resp.getBody().getRet().getOrderId()))
                        .isExecuted(0 == resp.getBody().getRet().getOrderId())
                        .build()
        );
    }

    @Override
    public Optional<OrderDto> get(String id) {
        WexGetRequest request = new WexGetRequest(nonce(), GET, Long.valueOf(id));
        ResponseEntity<WexGetResponse> resp = cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase(),
                        HttpMethod.POST,
                        new HttpEntity<>(request, signer.sign(request)),
                        WexGetResponse.class);

        checkResponse(resp.getBody());

        return resp.getBody().mapTo();
    }

    @Override
    public List<OrderDto> getOpen(TradingCurrency from, TradingCurrency to) {
        WexGetOpenRequest request = new WexGetOpenRequest(nonce(), GET_OPEN);
        ResponseEntity<WexGetOpenResponse> resp = cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase(),
                        HttpMethod.POST,
                        new HttpEntity<>(request, signer.sign(request)),
                        WexGetOpenResponse.class);

        checkResponse(resp.getBody());

        return resp.getBody().mapTo();
    }

    @Override
    public void cancel(String id) {
        WexCancelOrderRequest request = new WexCancelOrderRequest(nonce(), CANCEL, Long.valueOf(id));
        ResponseEntity<WexCancelOrderResponse> resp = cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase(),
                        HttpMethod.POST,
                        new HttpEntity<>(request, signer.sign(request)),
                        WexCancelOrderResponse.class);

        checkResponse(resp.getBody());

        log.info("Cancel request completed {}", resp.getBody());
    }

    @Override
    public void withdraw(TradingCurrency currency, BigDecimal amount, String destination) {
        // NOTE: WEX requires special API key permissions for doing that
        WexWithdrawRequest request = new WexWithdrawRequest(
                nonce(),
                WITHDRAW,
                cfg.getCustomResponseCurrencyMapping()
                        .getOrDefault(currency.getCode(), currency.getCode()).toUpperCase(),
                amount,
                destination
        );

        ResponseEntity<WexWithdrawResponse> resp = cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase(),
                        HttpMethod.POST,
                        new HttpEntity<>(request, signer.sign(request)),
                        WexWithdrawResponse.class);

        checkResponse(resp.getBody());

        log.info("Withdraw request completed {}", resp.getBody());
    }

    @Override
    @Asynchronous
    @IgnoreRateLimited
    public String name() {
        return WEX;
    }

    @VisibleForTesting
    protected int nonce() {
        return nonce(1);
    }

    private int nonce(int step) {
        // most probably we don't do 1 request per ms, so initial value will be always fine
        return (int) NONCE.addAndGet(step);
    }

    private void checkResponse(BaseWexResponse<?> response) {
        if (!response.isOk()) {
            // parse 'invalid nonce parameter; on key:2072397085, you sent:'1522253970', you should send:2072397086'
            if (response.getError().contains("nonce")) {
                NONCE.set(Long.valueOf(response.getError().split(":")[3]));
            }
        }

        response.selfAssert();
    }
}
