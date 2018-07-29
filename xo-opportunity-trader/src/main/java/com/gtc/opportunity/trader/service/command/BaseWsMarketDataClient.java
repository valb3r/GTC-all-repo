package com.gtc.opportunity.trader.service.command;

import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.gtc.model.provider.OrderBook;
import com.gtc.model.provider.SubscribeDto;
import com.gtc.opportunity.trader.config.WsConfig;
import com.gtc.ws.BaseWebsocketClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.gtc.opportunity.trader.config.Const.Ws.WS_RECONNECT_S;

/**
 * Created by Valentyn Berezin on 16.06.18.
 */
@Slf4j
public abstract class BaseWsMarketDataClient {

    private final BaseWebsocketClient wsClient;
    private final ObjectReader bookReader;

    private final Supplier<List<String>> clientNames;
    private final Consumer<OrderBook> handleBook;

    BaseWsMarketDataClient(
            WsConfig wsConfig,
            ObjectMapper objectMapper,
            Supplier<List<String>> clientNames,
            Consumer<OrderBook> handleBook) {
        this.wsClient = new BaseWebsocketClient(
                new BaseWebsocketClient.Config(
                        wsConfig.getMarket(),
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
        this.clientNames = clientNames;
        this.handleBook = handleBook;
    }

    @Scheduled(fixedDelayString = WS_RECONNECT_S)
    public void connectReconnect() {
        if (!wsClient.isDisconnected()) {
            return;
        }

        wsClient.connect(new HashMap<>());
    }

    @Transactional(readOnly = true)
    public void subscribeOnConnect(RxObjectEventConnected conn) {
        clientNames.get()
                .forEach(name ->
                    BaseWebsocketClient.sendIfNotNull(conn, new SubscribeDto(name, SubscribeDto.Mode.BOOK))
                );
    }

    @SneakyThrows
    private void handleJsonObject(JsonNode node) {
        if (!node.hasNonNull("meta")) {
            return;
        }

        OrderBook book = bookReader.readValue(node);
        handleBook.accept(book);
    }
}
