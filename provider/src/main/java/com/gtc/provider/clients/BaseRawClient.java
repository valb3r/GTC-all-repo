package com.gtc.provider.clients;

import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtc.meta.CurrencyPair;
import com.gtc.model.provider.Bid;
import com.gtc.ws.BaseWebsocketClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Raw websocket client, to deal with no-protocol WS.
 */
public abstract class BaseRawClient implements WsClient {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final String name;
    protected final ObjectMapper objectMapper;
    protected final Map<ChannelDto, Map<String, Bid>> market = new ConcurrentHashMap<>();
    protected final Map<ChannelDto, Ticker> ticker = new ConcurrentHashMap<>();

    private final BaseWebsocketClient client;

    private final AtomicReference<RxObjectEventConnected> conn = new AtomicReference<>();

    public BaseRawClient(String wsPath, String name, int disconnectIfInactiveS, ObjectMapper mapper) {
        this.name = name;
        this.objectMapper = mapper;
        this.client = new BaseWebsocketClient(
                new BaseWebsocketClient.Config(wsPath, name, disconnectIfInactiveS, mapper, log),
                new BaseWebsocketClient.Handlers(this::attachToBidsAndTicker, this::parseEventDto, this::parseArray)
        );
    }

    @SneakyThrows
    public void connect(Map<String, String> headers) {
        client.connect(headers);
    }

    @Override
    public boolean isDisconnected() {
        return client.isDisconnected();
    }

    @Override
    public MarketDto market() {
        Map<CurrencyPair, Collection<Bid>> byCurrency = new HashMap<>();
        Map<CurrencyPair, MarketDto.Ticker> allTickers = new HashMap<>();
        market.forEach((curr, mrkt) -> byCurrency.put(curr.getPair(), mrkt.values()));
        ticker.forEach((curr, value) -> allTickers.put(
                curr.getPair(),
                new MarketDto.Ticker(value.getTimestampMillis(), value.getValue())
        ));

        return new MarketDto(
                byCurrency,
                allTickers
        );
    }

    @Override
    public long connectedAtTimestamp() {
        return client.getConnectedAtTimestamp();
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public void resubscribe() {
        RxObjectEventConnected connected = conn.get();
        if (isDisconnected() || null == connected) {
            return;
        }

        market.clear();
        ticker.clear();
        attachToBidsAndTicker(connected);
    }

    protected void attachToBidsAndTicker(RxObjectEventConnected evt) {
        conn.set(evt);
        bidConfig().forEach((key, value) ->
                BaseWebsocketClient.sendIfNotNull(evt, subscribeBidEvent(key))
        );

        tickerConfig().forEach((key, value) ->
                BaseWebsocketClient.sendIfNotNull(evt, subscribeTickerEvent(key))
        );
    }

    protected abstract Map<String, CurrencyPair> bidConfig();

    protected abstract Map<String, CurrencyPair> tickerConfig();

    protected abstract Object subscribeTickerEvent(String symbol);

    protected abstract Object subscribeBidEvent(String symbol);

    protected abstract void parseEventDto(JsonNode node);

    protected abstract void parseArray(JsonNode node);

    @Getter
    @RequiredArgsConstructor
    protected static class Ticker {

        private final long timestampMillis = System.currentTimeMillis();
        private final double value;
    }
}
