package com.gtc.provider.clients.okex;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import com.gtc.provider.clients.BaseRawClient;
import com.gtc.provider.clients.ChannelDto;
import com.gtc.provider.clients.okex.dto.OkexOrderBookResponse;
import com.gtc.provider.clients.okex.dto.OkexRequest;
import com.gtc.provider.clients.okex.dto.OkexResponse;
import com.gtc.provider.clients.okex.dto.OkexTickerResponse;
import com.gtc.provider.config.OkexConf;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gtc.provider.config.Const.OKEX;

/**
 * Created by mikro on 12.01.2018.
 */
@Component
public class OkexClient extends BaseRawClient {

    private String EVENT_PREFIX = "ok_sub_spot_";

    private String SUBSCRIBE_EVENT = "addChannel";
    private String TICKER_EVENT = "ticker";
    private String ORDERBOOK_EVENT = "depth";

    private String ASKS_ALIAS = "asks";
    private String BIDS_ALIAS = "bids";

    private OkexConf conf;

    public OkexClient(OkexConf conf) {
        super(
                conf.getWs2().getRoot(),
                OKEX,
                conf.getDisconnectIfInactiveS(),
                new ObjectMapper(new JsonFactory())
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
        String channel = EVENT_PREFIX + symbol + "_" + TICKER_EVENT;
        return new OkexRequest(SUBSCRIBE_EVENT, channel);
    }

    @Override
    protected Object subscribeBidEvent(String symbol) {
        String channel = EVENT_PREFIX + symbol + "_" + ORDERBOOK_EVENT;
        return new OkexRequest(SUBSCRIBE_EVENT, channel);
    }

    @SneakyThrows
    @Override
    protected void parseEventDto(JsonNode node) {
        OkexResponse response = objectMapper.reader()
                .readValue(node.traverse(), OkexResponse.class);
        String type = response.getType();
        if (TICKER_EVENT.equals(type)) {
            handleTicker(response);
        } else if (ORDERBOOK_EVENT.equals(type)) {
            handleOrderBook(response);
        }
    }

    @SneakyThrows
    private void handleTicker(OkexResponse response) {
        String symbol = response.getSymbol();
        OkexTickerResponse evt = objectMapper.reader()
                .readValue(response.getData().traverse(), OkexTickerResponse.class);
        ticker.put(
                new ChannelDto(
                        symbol,
                        conf.getSymbol().getTicker().get(symbol)),
                new Ticker(evt.getLast())
        );
    }

    @SneakyThrows
    private void handleOrderBook(OkexResponse response) {
        String symbol = response.getSymbol();
        JsonNode data = response.getData();
        OkexOrderBookResponse evt = objectMapper.reader()
                .readValue(data.traverse(), OkexOrderBookResponse.class);
        if (data.get(ASKS_ALIAS) != null) {
            handleOrderBookType(evt.getAsks(), symbol, true);
        }
        if (data.get(BIDS_ALIAS) != null) {
            handleOrderBookType(evt.getBids(), symbol, false);
        }
    }

    private void handleOrderBookType(double[][] list, String symbol, boolean isSell) {
        for (double[] item : list) {
            double price = item[0];
            double amount = getAmount(item[1], isSell);
            if (amount == 0) {
                removeOrderBook(price, symbol);
            } else {
                addOrderBook(price, amount, symbol);
            }
        }
    }

    private void addOrderBook(double price, double amount, String symbol) {
        ChannelDto channel = new ChannelDto(symbol, conf.getSymbol().getBid().get(symbol));
        String id = String.valueOf(price);
        market.computeIfAbsent(channel, idKey -> new ConcurrentHashMap<>())
                .put(id, new Bid(id, amount, price, price));
    }

    private void removeOrderBook(double price, String symbol) {
        ChannelDto channel = new ChannelDto(symbol, conf.getSymbol().getBid().get(symbol));
        market.computeIfAbsent(channel, id -> new ConcurrentHashMap<>()).remove(String.valueOf(price));
    }

    private double getAmount(double amount, boolean isSell) {
        if (isSell) {
            return -amount;
        }
        return amount;
    }

    @Override
    protected void parseArray(JsonNode node) {
        node.forEach(this::parseEventDto);
    }
}
