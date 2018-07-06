package com.gtc.persistor.service;

import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtc.model.provider.OrderBook;
import com.gtc.model.provider.SubscribeDto;
import com.gtc.persistor.config.WsConfig;
import com.gtc.ws.BaseWebsocketClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;

import static com.gtc.persistor.config.Const.Ws.WS_RECONNECT_S;

/**
 * Created by Valentyn Berezin on 16.06.18.
 */
@Slf4j
@Service
public class WsMarketDataClient {

    private final WsConfig cfg;
    private final OrderBookRepository bookRepository;
    private final ObjectMapper mapper;
    private final BaseWebsocketClient wsClient;

    public WsMarketDataClient(
            WsConfig wsConfig,
            OrderBookRepository bookRepository,
            ObjectMapper objectMapper) {
        this.cfg = wsConfig;
        this.bookRepository = bookRepository;
        this.mapper = objectMapper;
        this.wsClient = new BaseWebsocketClient(
                new BaseWebsocketClient.Config(
                        wsConfig.getProvider(),
                        "market",
                        wsConfig.getDisconnectIfInactiveS(),
                        objectMapper, log
                ),
                new BaseWebsocketClient.Handlers(
                        this::subscribeOnConnect,
                        this::handleJsonObject,
                        node -> {}
                )
        );
    }

    @Scheduled(fixedDelayString = WS_RECONNECT_S)
    public void connectReconnect() {
        if (!wsClient.isDisconnected()) {
            return;
        }

        wsClient.connect(new HashMap<>());
    }

    private void subscribeOnConnect(RxObjectEventConnected conn) {
        cfg.getMarketsToSubscribe()
                .forEach(name ->
                        BaseWebsocketClient.sendIfNotNull(conn, new SubscribeDto(name, SubscribeDto.Mode.BOOK))
                );
    }

    @SneakyThrows
    private void handleJsonObject(JsonNode node) {
        if (!node.hasNonNull("meta")) {
            return;
        }

        OrderBook book = mapper.readValue(node.toString(), OrderBook.class);
        bookRepository.storeOrderBook(book);
    }
}
