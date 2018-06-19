package com.gtc.provider.clients.wex;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import com.gtc.provider.clients.BaseRawClient;
import com.gtc.provider.clients.ChannelDto;
import com.gtc.provider.clients.common.pusher.dto.PusherSubscribe;
import com.gtc.provider.clients.wex.dto.WexOrderBookResponse;
import com.gtc.provider.clients.wex.dto.WexTickerResponse;
import com.gtc.provider.config.WexConf;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gtc.provider.config.Const.WEX;

/**
 * Created by mikro on 16.01.2018.
 */
@Component
public class WexClient extends BaseRawClient {

    private static final String DEPTH_EVENT = "depth";
    private static final String TICKER_EVENT = "trades";

    private WexConf conf;

    public WexClient(WexConf conf) {
        super(
                conf.getWs2().getRoot(),
                WEX,
                conf.getDisconnectIfInactiveS(), new ObjectMapper(new JsonFactory())
                        .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
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
        return new PusherSubscribe(new PusherSubscribe.ToChannel(symbol));
    }

    @Override
    protected Object subscribeBidEvent(String symbol) {
        return new PusherSubscribe(new PusherSubscribe.ToChannel(symbol));
    }

    @SneakyThrows
    @Override
    protected void parseEventDto(JsonNode node) {
        String event = node.get("event").asText();
        if (DEPTH_EVENT.equals(event)) {
            parseOrderBook(node);
        } else if (TICKER_EVENT.equals(event)) {
            parseTicker(node);
        }
    }

    @SneakyThrows
    private void parseTicker(JsonNode node) {
        WexTickerResponse dto = objectMapper.reader()
                .readValue(node.traverse(), WexTickerResponse.class);
        String[][] data = objectMapper.readerFor(String[][].class)
                .readValue(dto.getData());
        String symbol = dto.getChannel();
        double price = Double.parseDouble(data[0][1]);
        ticker.put(
                new ChannelDto(
                        symbol,
                        conf.getSymbol().getTicker().get(symbol)),
                new Ticker(price));
    }

    @SneakyThrows
    private void parseOrderBook(JsonNode node) {
        WexOrderBookResponse dto = objectMapper.reader()
                .readValue(node.traverse(), WexOrderBookResponse.class);
        WexOrderBookResponse.WexOrderBookItem data = objectMapper
                .readerFor(WexOrderBookResponse.WexOrderBookItem.class)
                .readValue(dto.getData());
        String symbol = dto.getChannel();
        ChannelDto channelDto = new ChannelDto(
                symbol,
                conf.getSymbol().getBid().get(symbol));
        Map<String, Bid> channel = market.computeIfAbsent(
                channelDto,
                id -> new ConcurrentHashMap<>());
        data.getAsk().forEach((double[] list) -> handleOrderBookItem(channel, list, true));
        data.getBid().forEach((double[] list) -> handleOrderBookItem(channel, list, false));
    }

    private void handleOrderBookItem(Map<String, Bid> channel, double[] list, boolean isSell) {
        double amount = list[1];
        double price = list[0];
        String id = String.valueOf(price);
        if (0.0 == amount) {
            channel.remove(id);
            return;
        }
        channel.put(id, new Bid(id, getAmount(amount, isSell), price, price));
    }

    private double getAmount(double amount, boolean isSell) {
        return isSell ? -amount : amount;
    }

    @Override
    protected void parseArray(JsonNode node) {
        node.forEach(this::parseEventDto);
    }
}
