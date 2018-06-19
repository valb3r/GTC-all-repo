package com.gtc.provider.clients.gdax;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.ImmutableList;
import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import com.gtc.provider.clients.BaseRawClient;
import com.gtc.provider.clients.ChannelDto;
import com.gtc.provider.clients.gdax.dto.*;
import com.gtc.provider.config.GdaxConf;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gtc.provider.config.Const.GDAX;

/**
 * Created by mikro on 04.01.2018.
 */
@Component
public class GdaxClient extends BaseRawClient {

    private static final String SUBSCRIBE = "subscribe";
    private static final String SELL = "sell";

    private static final String L2_EVENT = "level2";

    private static final String TYPE_ALIAS = "type";

    private static final String TICKER_TYPE = "ticker";
    private static final String SNAPSHOT_TYPE = "snapshot";
    private static final String UPDATE_TYPE = "l2update";

    private final GdaxConf conf;

    public GdaxClient(GdaxConf conf) {
        super(
                conf.getWs2().getRoot(),
                GDAX,
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
        Object channel = new GdaxChannelItem(TICKER_TYPE, ImmutableList.of(symbol));
        return new GdaxOp(SUBSCRIBE, ImmutableList.of(symbol), ImmutableList.of(channel));
    }

    @Override
    protected Object subscribeBidEvent(String symbol) {
        return new GdaxOp(SUBSCRIBE, ImmutableList.of(symbol), ImmutableList.of(L2_EVENT));
    }

    @Override
    @SneakyThrows
    protected void parseEventDto(JsonNode node) {
        String type = node.get(TYPE_ALIAS).asText();
        if (TICKER_TYPE.equals(type)) {
            handleTicker(node);
        } else if (SNAPSHOT_TYPE.equals(type)) {
            handleSnapshot(node);
        } else if (UPDATE_TYPE.equals(type)) {
            handleBidUpdate(node);
        }
    }

    @Override
    protected void parseArray(JsonNode node) {
        // NOP
    }

    @SneakyThrows
    private void handleTicker(JsonNode node) {
        GdaxTickerResponse dto = objectMapper.reader().readValue(node.traverse(), GdaxTickerResponse.class);
        String symbol = dto.getProductId();
        ticker.put(
                new ChannelDto(symbol, getPair(symbol)),
                new Ticker(dto.getPrice()));
    }

    @SneakyThrows
    private void handleSnapshot(JsonNode node) {
        GdaxSnapshotResponse dto = objectMapper.reader().readValue(node.traverse(), GdaxSnapshotResponse.class);
        String symbol = dto.getProductId();
        ChannelDto channel = new ChannelDto(symbol, getPair(symbol));
        handleSnapshotType(dto.getAsks(), channel, true);
        handleSnapshotType(dto.getBids(), channel, false);
    }

    private void handleSnapshotType(double[][] list, ChannelDto channel, boolean isSell) {
        for (double[] snapshotItem : list) {
            double price = snapshotItem[0];
            double amount = getAmount(snapshotItem[1], isSell);
            String id = String.valueOf(price);
            addBid(id, amount, price, channel);
        }
    }

    @SneakyThrows
    private void handleBidUpdate(JsonNode data) {
        GdaxSnapshotUpdateResponse dto = objectMapper
                .reader()
                .readValue(data.traverse(), GdaxSnapshotUpdateResponse.class);

        String[][] changes = dto.getChanges();
        String symbol = dto.getProductId();
        ChannelDto channel = new ChannelDto(symbol, getPair(symbol));

        for (String[] change : changes) {
            String type = change[0];
            double price = Double.parseDouble(change[1]);
            String id = String.valueOf(price);
            double amount = getAmount(Double.parseDouble(change[2]), SELL.equals(type));
            if (amount == 0) {
                removeBid(id, channel);
            } else {
                addBid(id, amount, price, channel);
            }
        }
    }

    private void addBid(String id, double amount, double price, ChannelDto channel) {
        market.computeIfAbsent(channel, mapId -> new ConcurrentHashMap<>())
                .put(id, new Bid(id, amount, price, price));
    }

    private void removeBid(String id, ChannelDto channel) {
        market.computeIfAbsent(channel, mapId -> new ConcurrentHashMap<>())
                .remove(id);
    }

    private double getAmount(double amount, boolean isSell) {
        return isSell ? -amount : amount;
    }

    private CurrencyPair getPair(String symbol) {
        return conf.getSymbol().getTicker().get(symbol);
    }
}
