package com.gtc.opportunity.trader.service.command;

import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.gtc.model.provider.OrderBook;
import com.gtc.model.provider.ProviderSubsDto;
import com.gtc.opportunity.trader.config.WsConfig;
import com.gtc.ws.BaseWebsocketClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.gtc.opportunity.trader.config.Const.Ws.WS_RECONNECT_S;

/**
 * Created by Valentyn Berezin on 16.06.18.
 */
@Slf4j
public abstract class BaseWsProviderClient {

    private final BaseWebsocketClient wsClient;
    private final ObjectReader bookReader;

    private final Supplier<List<? extends ProviderSubsDto>> subscribe;
    private final Consumer<OrderBook> handleBook;

    private AtomicInteger lastConnVersion = new AtomicInteger();

    BaseWsProviderClient(
            String name,
            WsConfig wsConfig,
            ObjectMapper objectMapper,
            Supplier<List<? extends ProviderSubsDto>> subscribe,
            Consumer<OrderBook> handleBook) {
        this.wsClient = new BaseWebsocketClient(
                new BaseWebsocketClient.Config(
                        wsConfig.getMarket(),
                        name,
                        wsConfig.getDisconnectIfInactiveS(),
                        objectMapper, log
                ),
                new BaseWebsocketClient.Handlers(
                        this::subscribeOnConnect,
                        this::handleJsonObject,
                        node -> {
                        }
                )
        );
        this.bookReader = objectMapper.readerFor(OrderBook.class);
        this.subscribe = subscribe;
        this.handleBook = handleBook;
    }

    @Scheduled(fixedDelayString = WS_RECONNECT_S)
    public void connectReconnect() {

        if (!wsClient.isDisconnected()) {

            if (lastConnVersion.get() != computeConnectionVersion(subscribe.get())) {
                log.info("Connection version change detected, asking WS to disconnect");
                wsClient.disconnect();
            }
            return;
        }

        wsClient.connect(new HashMap<>());
    }

    @Transactional(readOnly = true)
    public void subscribeOnConnect(RxObjectEventConnected conn) {
        List<? extends ProviderSubsDto> subs = subscribe.get();
        lastConnVersion.set(computeConnectionVersion(subs));
        subs.forEach(it ->
                BaseWebsocketClient.sendIfNotNull(conn, it)
        );
    }

    private int computeConnectionVersion(List<? extends ProviderSubsDto> subscribe) {
        return subscribe.hashCode();
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
