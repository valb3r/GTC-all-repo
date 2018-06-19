package com.gtc.provider.clients.zb;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import com.gtc.provider.clients.BaseRawClient;
import com.gtc.provider.clients.ChannelDto;
import com.gtc.provider.clients.zb.dto.ZbEvent;
import com.gtc.provider.clients.zb.dto.ZbSubscribe;
import com.gtc.provider.config.ZbConfig;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gtc.provider.config.Const.ZB;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Component
public class ZbClient extends BaseRawClient {

    private static final String ADD_CHANNEL = "addChannel";

    private final ZbConfig conf;

    public ZbClient(ZbConfig conf) {
        super(
                conf.getWs2().getRoot(),
                ZB,
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
        return new ZbSubscribe(ADD_CHANNEL, symbol + "_ticker");
    }

    @Override
    protected Object subscribeBidEvent(String symbol) {
        return new ZbSubscribe(ADD_CHANNEL, symbol + "_depth");
    }

    @Override
    @SneakyThrows
    protected void parseEventDto(JsonNode node) {
        ZbEvent evt = objectMapper.reader().readValue(node.traverse(), ZbEvent.class);
        if (null != evt.getTicker()) {
            handleTicker(evt);
        } else {
            handleBids(evt);
        }
    }

    private void handleTicker(ZbEvent evt) {
        ticker.put(
                new ChannelDto(
                        evt.getChannel(),
                        conf.getSymbol().getTicker().get(evt.getChannel().split("_")[0])),
                new Ticker(Double.valueOf(evt.getTicker().getLast())));
    }

    private void handleBids(ZbEvent evt) {
        // TODO - looks like they use full market snapshot and not update, so using PUT...
        Map<String, Bid> channel = new ConcurrentHashMap<>();
        market.put(
                new ChannelDto(
                        evt.getChannel(),
                        conf.getSymbol().getTicker().get(evt.getChannel().split("_")[0])),
                channel);
        evt.getAsks().forEach(dto -> createBid(channel, dto, -1.0));
        evt.getBids().forEach(dto -> createBid(channel, dto, 1.0));
    }

    private void createBid(Map<String, Bid> channel, Double[] dto, double sign) {
        double amount = sign * dto[1];
        if (0.0 == amount) {
            channel.remove(String.valueOf(dto[0]));
            return;
        }

        // price is id
        channel.put(String.valueOf(dto[0]), new Bid(String.valueOf(dto[0]), amount, dto[0], dto[0]));
    }

    @Override
    protected void parseArray(JsonNode node) {
        // NOP
    }
}
