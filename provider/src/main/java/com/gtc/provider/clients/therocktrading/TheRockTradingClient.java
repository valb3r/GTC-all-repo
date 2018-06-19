package com.gtc.provider.clients.therocktrading;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.ImmutableMap;
import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import com.gtc.provider.clients.BaseRawClient;
import com.gtc.provider.clients.ChannelDto;
import com.gtc.provider.clients.common.pusher.dto.PusherEvent;
import com.gtc.provider.clients.common.pusher.dto.PusherSubscribe;
import com.gtc.provider.clients.therocktrading.dto.TheRockTradingOrderEvent;
import com.gtc.provider.clients.therocktrading.dto.TheRockTradingOrderSnapshotEvent;
import com.gtc.provider.clients.therocktrading.dto.TheRockTradingTickerEvent;
import com.gtc.provider.config.TheRockTradingConf;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gtc.provider.config.Const.THE_ROCK_TRADING;

/**
 * Created by Valentyn Berezin on 12.01.18.
 */
@Service
public class TheRockTradingClient extends BaseRawClient {

    private static final String DATA = "data";
    private static final String CURRENCY = "currency";
    private static final String LAST_TRADE = "last_trade";
    private static final String ORDERBOOK = "orderbook";
    private static final String ORDERBOOK_DIFF = "orderbook_diff";
    private static final String ASK = "ask";
    private static final String BID = "bid";

    private final TheRockTradingConf conf;

    public TheRockTradingClient(TheRockTradingConf conf) {
        super(
                conf.getWs2().getRoot(),
                THE_ROCK_TRADING,
                conf.getDisconnectIfInactiveS(),
                new ObjectMapper(new JsonFactory())
                        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false)
                        .configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, true)
                        .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false));
        this.conf = conf;
    }

    @Override
    protected Map<String, CurrencyPair> bidConfig() {
        return conf.getSymbol().getBid();
    }

    @Override
    protected Map<String, CurrencyPair> tickerConfig() {
        // It is global subs, so return fake entry
        return ImmutableMap.of("BTC", new CurrencyPair(null, null));
    }

    @Override
    protected Object subscribeTickerEvent(String symbol) {
        return new PusherSubscribe(new PusherSubscribe.ToChannel(CURRENCY));
    }

    @Override
    protected Object subscribeBidEvent(String symbol) {
        return new PusherSubscribe(new PusherSubscribe.ToChannel(symbol));
    }

    @Override
    @SneakyThrows
    protected void parseEventDto(JsonNode node) {
        if (node.get(DATA).isContainerNode()) {
            return;
        }

        PusherEvent dto = objectMapper.reader().readValue(node.traverse(), PusherEvent.class);

        if (LAST_TRADE.equals(dto.getEvent())) {
            handleTickerEvent(dto);
        } else if (ORDERBOOK.equals(dto.getEvent())) {
            handleBidSnapshotEvent(dto);
        } else if (ORDERBOOK_DIFF.equals(dto.getEvent())) {
            handleBidDiffEvent(dto);
        }
    }

    @SneakyThrows
    private void handleTickerEvent(PusherEvent data) {
        TheRockTradingTickerEvent evt = objectMapper
                .readerFor(TheRockTradingTickerEvent.class)
                .readValue(data.getData());

        // it spams us all symbols
        CurrencyPair symbol = conf.getSymbol().getTicker().get(evt.getSymbol());
        if (null == symbol) {
            return;
        }

        ticker.put(
                new ChannelDto(
                        evt.getSymbol(),
                        symbol),
                new Ticker(evt.getValue())
        );
    }

    @SneakyThrows
    private void handleBidDiffEvent(PusherEvent data) {
        TheRockTradingOrderEvent evt = objectMapper
                .readerFor(TheRockTradingOrderEvent.class)
                .readValue(data.getData());

        ChannelDto channelDto = new ChannelDto(
                data.getChannel(),
                conf.getSymbol().getBid().get(data.getChannel()));

        Map<String, Bid> channel = market.computeIfAbsent(
                channelDto,
                id -> new ConcurrentHashMap<>());

        if (ASK.equals(evt.getSide())) {
            createBid(channel, evt, -1.0);
        } else if (BID.equals(evt.getSide())) {
            createBid(channel, evt, 1.0);
        }
    }

    @SneakyThrows
    private void handleBidSnapshotEvent(PusherEvent data) {
        TheRockTradingOrderSnapshotEvent evt = objectMapper
                .readerFor(TheRockTradingOrderSnapshotEvent.class)
                .readValue(data.getData());

        ChannelDto channelDto = new ChannelDto(
                data.getChannel(),
                conf.getSymbol().getBid().get(data.getChannel()));

        Map<String, Bid> channel = market.computeIfAbsent(
                channelDto,
                id -> new ConcurrentHashMap<>());

        evt.getAsks().forEach(ask -> createBid(channel, ask, -1.0));
        evt.getBids().forEach(bid -> createBid(channel, bid, 1.0));
    }

    private void createBid(Map<String, Bid> channel, TheRockTradingOrderEvent value, double sign) {
        double amount = sign * value.getAmount();
        if (0.0 == amount) {
            channel.remove(String.valueOf(value.getPrice()));
            return;
        }

        // price is id
        channel.put(String.valueOf(value.getPrice()),
                new Bid(
                        String.valueOf(value.getPrice()),
                        amount,
                        value.getPrice(),
                        value.getPrice())
        );
    }

    @Override
    protected void parseArray(JsonNode node) {
        // NOP
    }
}
