package com.gtc.provider.clients.huobi;

import com.appunite.websocket.rx.RxMoreObservables;
import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import com.gtc.provider.clients.BaseRawClient;
import com.gtc.provider.clients.ChannelDto;
import com.gtc.provider.clients.huobi.dto.HuobiPong;
import com.gtc.provider.clients.huobi.dto.HuobiResponse;
import com.gtc.provider.clients.huobi.dto.HuobiSubscribeRequest;
import com.gtc.provider.config.HuobiConf;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.gtc.provider.config.Const.HUOBI;

/**
 * Created by mikro on 13.01.2018.
 */
@Component
public class HuobiClient extends BaseRawClient {

    private static final String PING = "ping";
    private static final String TICK = "tick";

    private final AtomicReference<RxObjectEventConnected> rxObjectEventConnected = new AtomicReference<>();
    private final HuobiConf conf;

    public HuobiClient(HuobiConf conf) {
        super(
                conf.getWs2().getRoot(),
                HUOBI,
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
        return new HuobiSubscribeRequest(symbol);
    }

    @Override
    protected Object subscribeBidEvent(String symbol) {
        return new HuobiSubscribeRequest(symbol);
    }

    @SneakyThrows
    @Override
    protected void parseEventDto(JsonNode node) {
        if (null != node.get(PING)) {
            handlePing(node);
        } else if (null != node.get(TICK)) {
            HuobiResponse evt = objectMapper.reader().readValue(node.traverse(), HuobiResponse.class);
            if (null != evt.getTick().getData()) {
                handleTicker(evt);
            } else {
                handleBids(evt);
            }
        }
    }

    @Override
    protected void parseArray(JsonNode node) {
        // NOP
    }

    @Override
    protected void attachToBidsAndTicker(RxObjectEventConnected evt) {
        rxObjectEventConnected.set(evt);
        super.attachToBidsAndTicker(evt);
    }

    private void handlePing(JsonNode node) {
        RxObjectEventConnected evt = rxObjectEventConnected.get();
        if (isDisconnected() || null == evt) {
            return;
        }

        RxMoreObservables
                .sendObjectMessage(evt.sender(), new HuobiPong(node.get(PING).asLong()))
                .subscribe();
    }

    private void handleTicker(HuobiResponse evt) {
        ticker.put(
                new ChannelDto(
                        evt.getChannel(),
                        conf.getSymbol().getTicker().get(evt.getChannel())),
                new Ticker(evt.getTick().getData().get(0).getPrice())
        );
    }

    private void handleBids(HuobiResponse evt) {
        // TODO - looks like they use full market snapshot and not update, so using PUT...
        Map<String, Bid> channel = new ConcurrentHashMap<>();
        market.put(
                new ChannelDto(
                        evt.getChannel(),
                        conf.getSymbol().getBid().get(evt.getChannel())),
                channel);
        evt.getTick().getAsks().forEach(ask -> createBid(channel, ask, -1.0));
        evt.getTick().getBids().forEach(bid -> createBid(channel, bid, 1.0));
    }

    private void createBid(Map<String, Bid> channel, double[] param, double sign) {
        double amount = sign * param[1];
        if (0.0 == amount) {
            channel.remove(String.valueOf(param[0]));
            return;
        }

        // price is id
        channel.put(String.valueOf(param[0]),
                new Bid(
                        String.valueOf(param[0]),
                        amount,
                        param[0],
                        param[0])
        );
    }
}
