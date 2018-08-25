package com.gtc.persistor.service;

import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.gtc.model.provider.OrderBook;
import com.gtc.model.provider.ProviderSubsDto;
import com.gtc.model.provider.SubscribeStreamDto;
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
    private final BaseWebsocketClient wsClient;

    private final ObjectReader bookReader;

    public WsMarketDataClient(
            WsConfig wsConfig,
            OrderBookRepository bookRepository,
            ObjectMapper objectMapper) {
        this.cfg = wsConfig;
        this.bookRepository = bookRepository;
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

        this.bookReader = objectMapper.readerFor(OrderBook.class);
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
                        BaseWebsocketClient.sendIfNotNull(conn, new SubscribeStreamDto(ProviderSubsDto.Mode.BOOK, name))
                );
    }

    @SneakyThrows
    private void handleJsonObject(JsonNode node) {
        if (!node.hasNonNull("meta")) {
            return;
        }

        OrderBook book = bookReader.readValue(node);
        bookRepository.storeOrderBook(book);
    }
}
