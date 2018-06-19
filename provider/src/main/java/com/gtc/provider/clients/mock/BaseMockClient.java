package com.gtc.provider.clients.mock;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import com.gtc.provider.clients.BaseRawClient;
import com.gtc.provider.clients.ChannelDto;
import com.gtc.provider.clients.mock.dto.BaseSubscribeDto;
import com.gtc.provider.clients.mock.dto.BookDto;
import com.gtc.provider.clients.mock.dto.SubsType;
import com.gtc.provider.clients.mock.dto.TickerDto;
import com.gtc.provider.config.MockExchngConf;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Valentyn Berezin on 09.03.18.
 */
public abstract class BaseMockClient extends BaseRawClient {

    private static final String PING = "ping";
    private static final String BOOK = "amount";

    private final MockExchngConf conf;

    public BaseMockClient(MockExchngConf conf, String name) {
        super(
                conf.getWs2().getRoot(),
                name,
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
        return new BaseSubscribeDto(SubsType.TICKER, symbol, name());
    }

    @Override
    protected Object subscribeBidEvent(String symbol) {
        return new BaseSubscribeDto(SubsType.BOOK, symbol, name());
    }

    @Override
    @SneakyThrows
    protected void parseEventDto(JsonNode node) {
        if (node.has(PING)) {
            return;
        }

        if (node.has(BOOK)) {
            BookDto evt = objectMapper.reader().readValue(node.traverse(), BookDto.class);
            handleBids(evt);
        } else {
            TickerDto evt = objectMapper.reader().readValue(node.traverse(), TickerDto.class);
            handleTicker(evt);
        }
    }

    @Override
    protected void parseArray(JsonNode node) {
        // NOP
    }

    private void handleTicker(TickerDto evt) {
        ticker.put(
                new ChannelDto(
                        evt.getSymbol(),
                        conf.getSymbol().getTicker().get(evt.getSymbol())),
                new Ticker(evt.getPrice().doubleValue())
        );
    }

    private void handleBids(BookDto evt) {
        market.computeIfAbsent(
                new ChannelDto(
                        evt.getSymbol(),
                        conf.getSymbol().getBid().get(evt.getSymbol())),
                id -> new ConcurrentHashMap<>()
        ).put(
                String.valueOf(evt.getPrice()),
                new Bid(
                        String.valueOf(evt.getPrice()),
                        evt.getAmount().doubleValue(),
                        evt.getPrice().doubleValue(),
                        evt.getPrice().doubleValue())
        );
    }
}
