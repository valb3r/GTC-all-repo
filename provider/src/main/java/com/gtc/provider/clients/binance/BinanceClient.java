package com.gtc.provider.clients.binance;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.*;
import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import com.gtc.provider.clients.BaseRawClient;
import com.gtc.provider.clients.ChannelDto;
import com.gtc.provider.clients.binance.dto.BinanceOrderBookResponse;
import com.gtc.provider.clients.binance.dto.BinanceStream;
import com.gtc.provider.clients.binance.dto.BinanceTickerResponse;
import com.gtc.provider.config.BinanceConf;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gtc.provider.config.Const.BINANCE;


/**
 * Created by mikro on 07.01.2018.
 */
@Component
public class BinanceClient extends BaseRawClient {

    private static final String CHANNEL_SEP = "@";
    private static final String TICKER_CHANNEL = "ticker";
    private static final String ORDERBOOK_CHANNEL = "depth";
    private final BinanceConf conf;

    public BinanceClient(BinanceConf conf) {
        super(
                conf.getWs2().getRoot() + "?streams=" + getRequestPath(conf),
                BINANCE,
                conf.getDisconnectIfInactiveS(),
                new ObjectMapper(new JsonFactory())
                        .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false)
                        .configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, true)
                        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                        .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false)
        );
        this.conf = conf;
    }

    private static String getRequestPath(BinanceConf conf) {
        Collection<String> tickers = getRequestTypePath(tickerConfig(conf).keySet(), TICKER_CHANNEL);
        Collection<String> ordersBook = getRequestTypePath(bidConfig(conf).keySet(), ORDERBOOK_CHANNEL);
        return Stream.of(tickers, ordersBook)
                .flatMap(Collection::stream)
                .collect(Collectors.joining("/"));
    }

    private static Collection<String> getRequestTypePath(Set<String> pairs, String channel) {
        return pairs
                .stream()
                .map((String item) -> item.toLowerCase() + CHANNEL_SEP + channel)
                .collect(Collectors.toList());
    }

    protected static Map<String, CurrencyPair> bidConfig(BinanceConf conf) {
        return conf.getSymbol().getBid();
    }

    protected static Map<String, CurrencyPair> tickerConfig(BinanceConf conf) {
        return conf.getSymbol().getTicker();
    }

    @Override
    protected Object subscribeTickerEvent(String symbol) {
        return null;
    }

    @Override
    protected Object subscribeBidEvent(String symbol) {
        return null;
    }

    @Override
    @SneakyThrows
    protected void parseEventDto(JsonNode node) {
        BinanceStream stream = objectMapper.reader()
                .readValue(node.traverse(), BinanceStream.class);
        String type = stream.getType();
        if (TICKER_CHANNEL.equals(type)) {
            handleTicker(node);
        } else if (ORDERBOOK_CHANNEL.equals(type)) {
            handleOrderBook(node);
        }
    }

    @SneakyThrows
    private void handleTicker(JsonNode node) {
        BinanceTickerResponse response = objectMapper.reader()
                .readValue(node.get("data").traverse(), BinanceTickerResponse.class);
        String symbol = response.getSymbol();
        ticker.put(
                new ChannelDto(symbol, conf.getSymbol().getTicker().get(symbol)),
                new Ticker(response.getPrice()));
    }

    @SneakyThrows
    private void handleOrderBook(JsonNode node) {
        BinanceOrderBookResponse response = objectMapper.reader()
                .readValue(node.get("data").traverse(), BinanceOrderBookResponse.class);
        String symbol = response.getSymbol();
        handleOrderBookType(response.getAsks(), symbol, true);
        handleOrderBookType(response.getBids(), symbol, false);
    }

    @SneakyThrows
    private void handleOrderBookType(JsonNode node, String symbol, boolean isSell) {
        node.forEach(bid -> {
            double amount = bid.get(1).asDouble();
            double price = bid.get(0).asDouble();
            if (amount == 0) {
                removeOrderBookItem(symbol, price);
            } else {
                addOrderBookItem(symbol, price, amount, isSell);
            }
        });
    }

    private void addOrderBookItem(String symbol, double price, double amount, boolean isSell) {
        ChannelDto channel = new ChannelDto(symbol, conf.getSymbol().getBid().get(symbol));
        market.computeIfAbsent(channel, id -> new ConcurrentHashMap<>())
                .put(String.valueOf(price), new Bid(
                        String.valueOf(price),
                        getAmount(amount, isSell),
                        price,
                        price
                ));
    }

    private void removeOrderBookItem(String symbol, double price) {
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
        // NOP
    }

    @Override
    protected Map<String, CurrencyPair> bidConfig() {
        return bidConfig(conf);
    }

    @Override
    protected Map<String, CurrencyPair> tickerConfig() {
        return tickerConfig(conf);
    }
}
