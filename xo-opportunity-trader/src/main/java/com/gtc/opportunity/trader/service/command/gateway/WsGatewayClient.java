package com.gtc.opportunity.trader.service.command.gateway;

import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableMap;
import com.gtc.model.gateway.BaseMessage;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.response.ErrorResponse;
import com.gtc.model.gateway.response.account.GetAllBalancesResponse;
import com.gtc.model.gateway.response.create.CreateOrderResponse;
import com.gtc.model.gateway.response.manage.CancelOrderResponse;
import com.gtc.model.gateway.response.manage.GetOrderResponse;
import com.gtc.model.gateway.response.manage.ListOpenOrdersResponse;
import com.gtc.opportunity.trader.config.WsConfig;
import com.gtc.ws.BaseWebsocketClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.gtc.opportunity.trader.config.Const.Ws.WS_RECONNECT_S;

/**
 * Created by Valentyn Berezin on 19.06.18.
 */
@Slf4j
@Component
public class WsGatewayClient {

    private final AtomicReference<RxObjectEventConnected> connected = new AtomicReference<>();
    private final BaseWebsocketClient client;
    private final ObjectMapper mapper;

    private final Map<String, Consumer<JsonNode>> handlers;
    private final Map<String, Consumer<ErrorResponse>> errorHandlers;

    private final Map<String, ObjectReader> readers = new ConcurrentHashMap<>();

    public WsGatewayClient(WsConfig wsConfig, ObjectMapper objectMapper, WsGatewayResponseListener respListener,
                           WsGatewayErrorResponseListener errListener) {
        this.mapper = objectMapper;
        this.client = new BaseWebsocketClient(
                new BaseWebsocketClient.Config(
                        wsConfig.getGateway(),
                        "gateway",
                        wsConfig.getDisconnectIfInactiveS(),
                        objectMapper,
                        log
                ),
                new BaseWebsocketClient.Handlers(
                        this::handleConnected,
                        this::handleMessage,
                        node -> {}
                )
        );

        this.handlers = ImmutableMap.<String, Consumer<JsonNode>>builder()
                .put(GetAllBalancesResponse.TYPE, m -> respListener.walletUpdate(read(m, GetAllBalancesResponse.class)))
                .put(CreateOrderResponse.TYPE, m -> respListener.createOrder(read(m, CreateOrderResponse.class)))
                .put(GetOrderResponse.TYPE, m -> respListener.byId(read(m, GetOrderResponse.class)))
                .put(ListOpenOrdersResponse.TYPE, m -> respListener.opened(read(m, ListOpenOrdersResponse.class)))
                .put(CancelOrderResponse.TYPE, m -> respListener.cancelled((read(m, CancelOrderResponse.class))))
                .put(ErrorResponse.TYPE, m -> handleError(read(m, ErrorResponse.class)))
                .build();

        this.errorHandlers = ImmutableMap.<String, Consumer<ErrorResponse>>builder()
                .put(CreateOrderCommand.TYPE, errListener::createOrderError)
                .put(GetOrderResponse.TYPE, errListener::manageError)
                .put(ListOpenOrdersResponse.TYPE, errListener::manageError)
                .put(GetAllBalancesResponse.TYPE, errListener::accountError)
                .build();
    }

    @Scheduled(fixedDelayString = WS_RECONNECT_S)
    public void connectReconnect() {
        if (!client.isDisconnected()) {
            return;
        }

        client.connect(new HashMap<>());
    }

    public void sendCommand(BaseMessage command) {
        RxObjectEventConnected conn = connected.get();
        if (null != conn) {
            command.setType(command.type());
            BaseWebsocketClient.sendIfNotNull(conn, command);
        }
    }

    @SneakyThrows
    private void handleConnected(RxObjectEventConnected connected) {
        this.connected.set(connected);
    }

    @SneakyThrows
    private void handleMessage(JsonNode node) {
        BaseMessage message = mapper.readValue(node.traverse(), BaseMessage.class);
        Consumer<JsonNode> handler = handlers.get(message.getType());
        if (null != handler) {
            handler.accept(node);
        }
    }

    @SneakyThrows
    private void handleError(ErrorResponse response) {
        Consumer<ErrorResponse> handler = errorHandlers.get(response.getOccurredOnType());
        if (null != handler) {
            handler.accept(response);
        }
    }

    @SneakyThrows
    private <T> T read(JsonNode message, Class<T> clazz) {
        return readers.computeIfAbsent(clazz.getCanonicalName(), id -> mapper.readerFor(clazz)).readValue(message);
    }
}
