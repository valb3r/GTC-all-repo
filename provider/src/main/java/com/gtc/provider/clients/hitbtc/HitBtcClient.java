package com.gtc.provider.clients.hitbtc;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import com.gtc.provider.clients.BaseRawClient;
import com.gtc.provider.clients.ChannelDto;
import com.gtc.provider.clients.hitbtc.dto.HitBtcOrderEvent;
import com.gtc.provider.clients.hitbtc.dto.HitBtcSubscribe;
import com.gtc.provider.clients.hitbtc.dto.HitBtcTickerEvent;
import com.gtc.provider.config.HitBtcConf;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gtc.provider.config.Const.HITBTC;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Component
public class HitBtcClient extends BaseRawClient {

    private static final String ORDERS_SUBSCRIBE = "subscribeOrderbook";
    private static final String TICKER_SUBSCRIBE = "subscribeTicker";
    private static final String BIDS_SNAPSHOT = "snapshotOrderbook";
    private static final String BIDS_UPDATE = "updateOrderbook";
    private static final String TICKER_MTD = "ticker";
    private static final String METHOD = "method";

    private final HitBtcConf conf;

    public HitBtcClient(HitBtcConf conf) {
        super(
                conf.getWs2().getRoot(),
                HITBTC,
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
        return new HitBtcSubscribe(TICKER_SUBSCRIBE, symbol);
    }

    @Override
    protected Object subscribeBidEvent(String symbol) {
        return new HitBtcSubscribe(ORDERS_SUBSCRIBE, symbol);
    }

    @Override
    protected void parseEventDto(JsonNode node) {
        if (null == node.get(METHOD)) {
            return;
        }

        if (TICKER_MTD.equals(node.get(METHOD).asText())) {
            handleTicker(node);
        } else if (BIDS_SNAPSHOT.equals(node.get(METHOD).asText())
                || BIDS_UPDATE.equals(node.get(METHOD).asText())) {
            handleBidEvent(node);
        }
    }

    @SneakyThrows
    private void handleTicker(JsonNode node) {
        HitBtcTickerEvent evt = objectMapper.reader().readValue(node.traverse(), HitBtcTickerEvent.class);
        ticker.put(
                new ChannelDto(
                        evt.getParams().getSymbol(),
                        conf.getSymbol().getTicker().get(evt.getParams().getSymbol())),
                new Ticker(Double.valueOf(evt.getParams().getLast()))
        );
    }

    @SneakyThrows
    private void handleBidEvent(JsonNode node) {
        HitBtcOrderEvent evt = objectMapper.reader().readValue(node.traverse(), HitBtcOrderEvent.class);
        ChannelDto channelDto = new ChannelDto(
                evt.getParams().getSymbol(),
                conf.getSymbol().getBid().get(evt.getParams().getSymbol()));

        if (BIDS_SNAPSHOT.equals(evt.getMethod())) {
            market.remove(channelDto);
        }

        Map<String, Bid> channel = market.computeIfAbsent(
                channelDto,
                id -> new ConcurrentHashMap<>());
        evt.getParams().getAsk().forEach(ask -> createBid(channel, ask, -1.0));
        evt.getParams().getBid().forEach(bid -> createBid(channel, bid, 1.0));
    }

    private void createBid(Map<String, Bid> channel, HitBtcOrderEvent.Params.Order param, double sign) {
        double amount = sign * Double.valueOf(param.getSize());
        if (0.0 == amount) {
            channel.remove(param.getPrice());
            return;
        }

        // price is id
        channel.put(param.getPrice(), new Bid(param.getPrice(), amount, Double.valueOf(param.getPrice()),
                Double.valueOf(param.getPrice())));
    }

    @Override
    protected void parseArray(JsonNode node) {
        // NOP
    }
}
