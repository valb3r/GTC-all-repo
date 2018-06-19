package com.gtc.provider.clients.bitstamp;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.base.CharMatcher;
import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import com.gtc.provider.clients.BaseRawClient;
import com.gtc.provider.clients.ChannelDto;
import com.gtc.provider.clients.bitstamp.dto.BitstampOrderEvent;
import com.gtc.provider.clients.bitstamp.dto.BitstampTickerEvent;
import com.gtc.provider.clients.common.pusher.dto.PusherSubscribe;
import com.gtc.provider.config.BitstampConf;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gtc.provider.config.Const.BITSTAMP;

/**
 * Created by Valentyn Berezin on 07.01.18.
 */
@Service
public class BitstampClient extends BaseRawClient {

    private static final String EVENT = "event";
    private static final String TRADE = "trade";
    private static final String DATA = "data";
    private static final String LIVE_TRADES = "live_trades_";
    private static final String ORDER_BOOK = "diff_order_book_";

    private final BitstampConf conf;

    public BitstampClient(BitstampConf conf) {
        super(
                conf.getWs2().getRoot(),
                BITSTAMP,
                conf.getDisconnectIfInactiveS(),
                new ObjectMapper(new JsonFactory())
                        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false)
                        .configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, true)
                        .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false)
        );
        this.conf = conf;
    }

    @Override
    protected Map<String, CurrencyPair> bidConfig() {
        return conf.getSymbol().getBid();
    }

    @Override
    protected Map<String, CurrencyPair> tickerConfig() {
        return conf.getSymbol().getTicker();
    }

    @Override
    protected Object subscribeTickerEvent(String symbol) {
        return new PusherSubscribe(new PusherSubscribe.ToChannel(trimChannel(LIVE_TRADES + symbol)));
    }

    @Override
    protected Object subscribeBidEvent(String symbol) {
        return new PusherSubscribe(new PusherSubscribe.ToChannel(trimChannel(ORDER_BOOK + symbol)));
    }

    @Override
    protected void parseEventDto(JsonNode node) {
        if (TRADE.equals(node.get(EVENT).asText())) {
            parseTickerEvent(node);
        } else if (DATA.equals(node.get(EVENT).asText())) {
            parseBidEvent(node);
        }
    }

    @Override
    protected void parseArray(JsonNode node) {
        // NOP
    }

    @SneakyThrows
    private void parseBidEvent(JsonNode node) {
        BitstampOrderEvent dto = objectMapper.reader().readValue(node.traverse(), BitstampOrderEvent.class);
        BitstampOrderEvent.InnerData data = objectMapper.readerFor(BitstampOrderEvent.InnerData.class)
                .readValue(dto.getData());

        ChannelDto channelDto = new ChannelDto(
                dto.getChannel(),
                conf.getSymbol().getBid().get(getChannelPair(dto.getChannel(), ORDER_BOOK)));

        Map<String, Bid> channel = market.computeIfAbsent(
                channelDto,
                id -> new ConcurrentHashMap<>());
        data.getAsks().forEach(ask -> createBid(channel, ask, -1.0));
        data.getBids().forEach(bid -> createBid(channel, bid, 1.0));
    }

    private void createBid(Map<String, Bid> channel, String[] param, double sign) {
        double amount = sign * Double.valueOf(param[1]);
        if (0.0 == amount) {
            channel.remove(param[0]);
            return;
        }

        // price is id
        channel.put(param[0],
                new Bid(
                        param[0],
                        amount,
                        Double.valueOf(param[0]),
                        Double.valueOf(param[0]))
        );
    }

    @SneakyThrows
    private void parseTickerEvent(JsonNode node) {
        BitstampTickerEvent dto = objectMapper.reader().readValue(node.traverse(), BitstampTickerEvent.class);
        BitstampTickerEvent.InnerData data = objectMapper.readerFor(BitstampTickerEvent.InnerData.class)
                .readValue(dto.getData());

        ticker.put(
                new ChannelDto(
                        dto.getChannel(),
                        conf.getSymbol().getTicker().get(getChannelPair(dto.getChannel(), LIVE_TRADES))),
                new Ticker(data.getPrice())
        );
    }

    private String trimChannel(String channel) {
        return CharMatcher.is('_').trimTrailingFrom(channel);
    }

    private String getChannelPair(String channel, String prefix) {
        if (channel.length() <= prefix.length()) {
            return "";
        }

        return channel.substring(prefix.length(), channel.length());
    }
}
