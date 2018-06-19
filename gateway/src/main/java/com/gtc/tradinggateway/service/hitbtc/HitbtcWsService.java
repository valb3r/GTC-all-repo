package com.gtc.tradinggateway.service.hitbtc;

import com.appunite.websocket.rx.RxMoreObservables;
import com.appunite.websocket.rx.object.ObjectWebSocketSender;
import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.fasterxml.jackson.databind.JsonNode;
import com.gtc.tradinggateway.config.HitbtcConfig;
import com.gtc.tradinggateway.meta.PairSymbol;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.BaseWsClient;
import com.gtc.tradinggateway.service.CreateOrder;
import com.gtc.tradinggateway.service.dto.OrderCreatedDto;
import com.gtc.tradinggateway.service.hitbtc.dto.HitbtcAuthRequestDto;
import com.gtc.tradinggateway.service.hitbtc.dto.HitbtcCreateRequestDto;
import com.gtc.tradinggateway.service.hitbtc.dto.HitbtcErrorDto;
import com.gtc.tradinggateway.util.DefaultInvertHandler;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.gtc.tradinggateway.config.Const.Clients.HITBTC;

/**
 * Validated basic functionality (create)
 * 05.03.2018
 */
@Service
public class HitbtcWsService extends BaseWsClient implements CreateOrder {

    private static final String ERROR_ALIAS = "error";
    private static final String AUTH_ALIAS = "auth";
    private static final String ID_ALIAS = "id";

    private final HitbtcConfig cfg;

    public HitbtcWsService(HitbtcConfig cfg) {
        super(cfg.getMapper());
        this.cfg = cfg;
    }

    @Override
    @SneakyThrows
    public Optional<OrderCreatedDto> create(String tryToAssignId, TradingCurrency from, TradingCurrency to,
                                            BigDecimal amount, BigDecimal price) {
        PairSymbol pair = cfg.pairFromCurrencyOrThrow(from, to);

        BigDecimal calcAmount = DefaultInvertHandler.amountFromOrig(pair, amount, price);
        BigDecimal calcPrice = DefaultInvertHandler.priceFromOrig(pair, price);
        String side = DefaultInvertHandler.amountToBuyOrSell(calcAmount);

        ObjectWebSocketSender sender = rxConnected.get().sender();
        HitbtcCreateRequestDto requestDto =
                new HitbtcCreateRequestDto(tryToAssignId, pair.getSymbol(), side, calcPrice, calcAmount.abs());

        log.info("Sent WS request {} to {}", requestDto, name());
        RxMoreObservables
                .sendObjectMessage(sender, requestDto)
                .subscribe();

        return Optional.empty();
    }

    @Override
    public String name() {
        return HITBTC;
    }

    @Override
    protected String getWs() {
        return cfg.getWsBase();
    }

    @Override
    protected Map<String, String> headers() {
        return new HashMap<>();
    }

    @Override
    protected void onConnected(RxObjectEventConnected conn) {
        login();
    }

    @Override
    protected boolean handledAsLogin(JsonNode node) {
        if (!AUTH_ALIAS.equals(node.get(ID_ALIAS).asText())) {
            return false;
        }

        isLoggedIn.set(true);
        return true;
    }

    @Override
    @SneakyThrows
    protected void parseEventDto(JsonNode node) {
        if (null != node.get(ERROR_ALIAS)) {
            HitbtcErrorDto error = cfg.getMapper().reader().readValue(node.traverse(), HitbtcErrorDto.class);
            throw new IllegalStateException(error.getError().getMessage());
        }
    }

    @Override
    protected void parseArray(JsonNode node) {
        // NOP
    }

    @Override
    protected void login() {
        ObjectWebSocketSender sender = rxConnected.get().sender();
        HitbtcAuthRequestDto requestDto = new HitbtcAuthRequestDto(cfg.getPublicKey(), cfg.getSecretKey());
        RxMoreObservables
                .sendObjectMessage(sender, requestDto)
                .subscribe();
    }
}
