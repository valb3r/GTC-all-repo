package com.gtc.provider.clients.bitfinex;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gtc.provider.clients.BaseRawClient;
import com.gtc.provider.clients.ChannelDto;
import com.gtc.provider.clients.bitfinex.dto.SubscribeEvent;
import com.gtc.provider.clients.bitfinex.dto.SubscribedEvent;
import com.gtc.provider.config.BitfinexConf;
import com.gtc.model.provider.Bid;
import com.gtc.meta.CurrencyPair;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gtc.provider.config.Const.BITFINEX;

/**
 * Created by Valentyn Berezin on 29.12.17.
 */
@Component
public class BitfinexClient extends BaseRawClient {

    private static final String EVENT_NAME = "event";
    private static final String BOOK_NAME = "book";
    private static final String TICKER_NAME = "ticker";

    private final BitfinexConf conf;

    public BitfinexClient(BitfinexConf conf) {
        super(
                conf.getWs2().getRoot(),
                BITFINEX,
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
    @SneakyThrows
    protected void parseEventDto(JsonNode node) {
        if (null == node.get(EVENT_NAME)) {
            return;
        }

        SubscribedEvent evt = objectMapper.reader().readValue(node.traverse(), SubscribedEvent.class);
        if (!"subscribed".equals(evt.getEvent())) {
            return;
        }

        if (BOOK_NAME.equals(evt.getChannel())) {
            market.put(
                    new ChannelDto(evt.getChanId(), conf.getSymbol().getBid().get(evt.getSymbol())),
                    new ConcurrentHashMap<>());
        } else if (TICKER_NAME.equals(evt.getChannel())) {
            ticker.put(
                    new ChannelDto(evt.getChanId(), conf.getSymbol().getTicker().get(evt.getSymbol())),
                    new Ticker(-1.0));
        }
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
        return new SubscribeEvent(TICKER_NAME, symbol);
    }

    @Override
    protected Object subscribeBidEvent(String symbol) {
        return new SubscribeEvent(BOOK_NAME, symbol);
    }

    @Override
    @SneakyThrows
    protected void parseArray(JsonNode node) {
        // avoid heartbeat
        if (!node.get(1).isArray()) {
            return;
        }

        ChannelDto dto = new ChannelDto(node.get(0).asLong());
        if (market.containsKey(dto)) {
            handleMarketResponse(node, dto);
        } else if (ticker.containsKey(dto)) {
            handleTickerResponse(node, dto);
        }
    }

    private void handleMarketResponse(JsonNode node, ChannelDto dto) {
        ArrayNode values = (ArrayNode) node.get(1);
        if (values.get(0).isArray()) {
            values.forEach(val -> handleValue(val, dto));
        } else {
            handleValue(values, dto);
        }
    }

    private void handleValue(JsonNode value, ChannelDto dto) {
        long id = value.get(0).asLong();
        double price = value.get(1).asDouble();
        double amount = value.get(2).asDouble();
        if (0.0 == amount || 0.0 == price) {
            market.get(dto).remove(String.valueOf(id));
        } else {
            market.get(dto).put(String.valueOf(id), new Bid(String.valueOf(id), amount, price, price));
        }
    }

    private void handleTickerResponse(JsonNode node, ChannelDto dto) {
        ArrayNode values = (ArrayNode) node.get(1);
        ticker.put(dto, new Ticker(values.get(6).asDouble()));
    }
}
