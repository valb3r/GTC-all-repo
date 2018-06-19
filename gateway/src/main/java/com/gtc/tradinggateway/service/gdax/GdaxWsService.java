package com.gtc.tradinggateway.service.gdax;

import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.fasterxml.jackson.databind.JsonNode;
import com.gtc.tradinggateway.config.GdaxConfig;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.BaseWsClient;
import com.gtc.tradinggateway.service.CreateOrder;
import com.gtc.tradinggateway.service.dto.OrderCreatedDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static com.gtc.tradinggateway.config.Const.Clients.GDAX;

/**
 * Created by Valentyn Berezin on 16.01.18.
 */
@Service
public class GdaxWsService extends BaseWsClient implements CreateOrder {

    private final GdaxConfig cfg;
    private final GdaxEncryptionService signer;

    public GdaxWsService(GdaxConfig cfg, GdaxEncryptionService signer) {
        super(cfg.getMapper());
        this.cfg = cfg;
        this.signer = signer;
    }

    @Override
    public Optional<OrderCreatedDto> create(String tryToAssignId, TradingCurrency from, TradingCurrency to,
                                            BigDecimal amount, BigDecimal price) {
        return Optional.empty();
    }

    @Override
    protected String getWs() {
        return cfg.getWsBase();
    }

    @Override
    protected void onConnected(RxObjectEventConnected conn) {
        // NOP
    }

    @Override
    protected void parseEventDto(JsonNode node) {
        // NOP
    }

    @Override
    protected void parseArray(JsonNode node) {
        // NOP
    }

    @Override
    protected void login() {}

    @Override
    protected boolean handledAsLogin(JsonNode node) {
        return false;
    }

    @Override
    protected Map<String, String> headers() {
        return signer.signingHeaders("", "", "");
    }

    @Override
    public String name() {
        return GDAX;
    }
}
