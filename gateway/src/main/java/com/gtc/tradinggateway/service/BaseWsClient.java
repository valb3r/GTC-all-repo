package com.gtc.tradinggateway.service;

import com.appunite.websocket.rx.RxWebSockets;
import com.appunite.websocket.rx.object.ObjectSerializer;
import com.appunite.websocket.rx.object.RxObjectWebSockets;
import com.appunite.websocket.rx.object.messages.RxObjectEvent;
import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.appunite.websocket.rx.object.messages.RxObjectEventMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.gtc.tradinggateway.aspect.ws.IgnoreWsReady;
import com.gtc.tradinggateway.aspect.ws.WsReady;
import com.gtc.tradinggateway.service.rxsupport.JacksonSerializer;
import com.gtc.tradinggateway.service.rxsupport.MoreObservables;
import com.newrelic.api.agent.NewRelic;
import lombok.SneakyThrows;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Valentyn Berezin on 16.01.18.
 */
@WsReady
public abstract class BaseWsClient {

    private static final String CONNECTS = "Custom/Connect/";
    private static final String MESSAGE = "Custom/Message/";

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final ObjectMapper objectMapper;
    protected final AtomicReference<RxObjectEventConnected> rxConnected = new AtomicReference<>();
    protected final AtomicBoolean isLoggedIn = new AtomicBoolean();

    public BaseWsClient(ObjectMapper mapper) {
        this.objectMapper = mapper;
    }

    @IgnoreWsReady
    public boolean isDisconnected() {
        return null == rxConnected.get();
    }

    @IgnoreWsReady
    public boolean isReady() {
        return !isDisconnected() && isLoggedIn.get();
    }

    @SneakyThrows
    protected void connect() {
        if (null != rxConnected.get()) {
            return;
        }

        log.info("Connecting");
        Request request = new Request.Builder()
                .get()
                .headers(Headers.of(headers()))
                .url(getWs())
                .build();

        Observable<RxObjectEvent> sharedConnection = getConnection(request);

        sharedConnection
                .compose(MoreObservables.filterAndMap(RxObjectEventConnected.class))
                .subscribe(onConn -> {
                    NewRelic.incrementCounter(CONNECTS + name());
                    rxConnected.set(onConn);
                    log.info("Connected");
                    login();
                    onConnected(onConn);
                });

        sharedConnection
                .compose(MoreObservables.filterAndMap(RxObjectEventMessage.class))
                .compose(RxObjectEventMessage.filterAndMap(JsonNode.class))
                .subscribe(this::handleInboundMessage);
    }

    private Observable<RxObjectEvent> getConnection(Request request) {
        ObjectSerializer serializer = new JacksonSerializer(objectMapper);
        return new RxObjectWebSockets(new RxWebSockets(new OkHttpClient(), request), serializer)
                .webSocketObservable()
                .doOnCompleted(() -> handleDisconnectEvt("Disconnected (completed)", null))
                .doOnError(throwable -> handleDisconnectEvt("Disconnected (exceptional)", throwable))
                .share();
    }

    protected void handleInboundMessage(JsonNode node) {
        NewRelic.incrementCounter(MESSAGE + name());
        try {
            if (handledAsLogin(node)) {
                return;
            }

            if (!node.isArray()) {
                parseEventDto(node);
            } else if (node.isArray()) {
                parseArray(node);
            }
        } catch (RuntimeException ex) {
            NewRelic.noticeError(ex, ImmutableMap.of("name", name()));
            log.error("Exception handling {}", node, ex);
        }
    }

    public abstract String name();
    protected abstract String getWs();
    protected abstract Map<String, String> headers();
    protected abstract void onConnected(RxObjectEventConnected conn);
    protected abstract void parseEventDto(JsonNode node);
    protected abstract void parseArray(JsonNode node);
    protected abstract void login();
    protected abstract boolean handledAsLogin(JsonNode node);

    private void handleDisconnectEvt(String reason, Throwable err) {
        rxConnected.set(null);

        if (null != err) {
            NewRelic.noticeError(err, ImmutableMap.of("name", name()));
            log.error(reason, err);
        } else {
            NewRelic.noticeError(name());
            log.error(reason);
        }
    }
}
