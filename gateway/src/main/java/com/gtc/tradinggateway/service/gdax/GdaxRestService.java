package com.gtc.tradinggateway.service.gdax;

import com.gtc.model.gateway.data.OrderDto;
import com.gtc.tradinggateway.aspect.rate.IgnoreRateLimited;
import com.gtc.tradinggateway.aspect.rate.RateLimited;
import com.gtc.tradinggateway.config.GdaxConfig;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.Account;
import com.gtc.tradinggateway.service.ManageOrders;
import com.gtc.tradinggateway.service.Withdraw;
import com.gtc.tradinggateway.service.gdax.dto.GdaxGetOrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.gtc.tradinggateway.config.Const.Clients.GDAX;

/**
 * Created by Valentyn Berezin on 16.01.18.
 */
@Service
@RequiredArgsConstructor
@RateLimited(ratePerMinute = "${app.gdax.ratePerM}", mode = RateLimited.Mode.CLASS)
public class GdaxRestService implements ManageOrders, Withdraw, Account {

    private static final String ORDERS = "/orders";

    private final GdaxConfig cfg;
    private final GdaxEncryptionService signer;

    @Override
    public Optional<OrderDto> get(String id) {
        String relUrl = ORDERS + "/" + id;
        ResponseEntity<GdaxGetOrderDto> resp = cfg.getRestTemplate()
                .exchange(
                        cfg.getRestBase() + relUrl,
                        HttpMethod.GET,
                        new HttpEntity<>(signer.restHeaders(relUrl, HttpMethod.GET.name(), "")),
                        GdaxGetOrderDto.class);
        if (resp.getStatusCode().is2xxSuccessful()) {
            return Optional.of(resp.getBody().map());
        }

        return Optional.empty();
    }

    @Override
    public List<OrderDto> getOpen(TradingCurrency from, TradingCurrency to) {
        return null;
    }

    @Override
    public void cancel(String id) {

    }

    @Override
    public Map<TradingCurrency, BigDecimal> balances() {
        return null;
    }

    @Override
    public void withdraw(TradingCurrency currency, BigDecimal amount, String destination) {

    }

    @Override
    @IgnoreRateLimited
    public String name() {
        return GDAX;
    }
}
