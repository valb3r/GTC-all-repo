package com.gtc.provider.clients.exx;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import com.gtc.provider.clients.BaseRawClient;
import com.gtc.provider.clients.ChannelDto;
import com.gtc.provider.clients.exx.dto.ExxOrderBookResponse;
import com.gtc.provider.clients.exx.dto.ExxRequest;
import com.gtc.provider.config.ExxConf;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gtc.provider.config.Const.EXX;

/**
 * Created by mikro on 14.01.2018.
 */
@Component
public class ExxClient extends BaseRawClient {

    private static final String ASK = "ASK";

    private static final String TICKER_EVENT = "T";
    private static final String ORDERBOOK_EVENT = "E";
    private static final String ORDERBOOK_SNAPSHOT_EVENT = "AE";

    private static final String TICKER_CHANNEL = "1_TRADE_";
    private static final String ORDERBOOK_CHANNEL = "1_ENTRUST_ADD_";

    private final ExxConf conf;

    public ExxClient(ExxConf conf) {
        super(
                conf.getWs2().getRoot(),
                EXX,
                conf.getDisconnectIfInactiveS(),
                new ObjectMapper(new JsonFactory())
                .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
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
        return conf.getSymbol().getTicker();
    }

    @Override
    protected Object subscribeTickerEvent(String symbol) {
        return new ExxRequest(TICKER_CHANNEL + symbol, 1);
    }

    @Override
    protected Object subscribeBidEvent(String symbol) {
        return new ExxRequest(ORDERBOOK_CHANNEL + symbol, 1);
    }

    @Override
    protected void parseEventDto(JsonNode node) {
        // NOP
    }

    @SneakyThrows
    private void handleTicker(JsonNode node) {
        String symbol = node.get(3).asText();
        double price = node.get(5).asDouble();
        ticker.put(
                new ChannelDto(
                        symbol,
                        conf.getSymbol().getTicker().get(symbol)),
                new Ticker(price));
    }

    @SneakyThrows
    private void handleOrderBook(JsonNode node) {
        ExxOrderBookResponse response = objectMapper.reader()
                .readValue(node.get(4).traverse(), ExxOrderBookResponse.class);
        String symbol = node.get(2).asText();
        response.getAsks().forEach(val -> handleOrderBookItem(symbol, val[0], val[1], true));
        response.getBids().forEach(val -> handleOrderBookItem(symbol, val[0], val[1], false));
    }

    private void handleOrderBookItem(String symbol, double price, double amount, boolean isSell) {
        ChannelDto channel = new ChannelDto(symbol, conf.getSymbol().getBid().get(symbol));
        if (amount == 0) {
            market.computeIfAbsent(channel, id -> new ConcurrentHashMap<>()).remove(String.valueOf(price));
            return;
        }
        String id = String.valueOf(price);
        market.computeIfAbsent(channel, idKey -> new ConcurrentHashMap<>())
                .put(id, new Bid(id, getAmount(amount, isSell), price, price));
    }

    private double getAmount(double amount, boolean isSell) {
        return isSell ? -amount : amount;
    }

    @Override
    protected void parseArray(JsonNode node) {
        JsonNode first = node.get(0);
        if (first.isArray()) {
            String evt = first.get(0).asText();
            if (ORDERBOOK_SNAPSHOT_EVENT.equals(evt)) {
                handleOrderBook(first);
            } else if (TICKER_EVENT.equals(evt)) {
                node.forEach(this::handleTicker);
            }
        } else {
            String evt = first.asText();
            if (ORDERBOOK_EVENT.equals(evt)) {
                String side = node.get(4).asText();
                handleOrderBookItem(
                        node.get(3).asText(),
                        node.get(5).asDouble(),
                        node.get(6).asDouble(),
                        ASK.equals(side));
            } else if (TICKER_EVENT.equals(evt)) {
                handleTicker(node);
            }
        }
    }
}
