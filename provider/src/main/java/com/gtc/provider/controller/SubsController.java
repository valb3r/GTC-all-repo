package com.gtc.provider.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtc.model.provider.ProviderSubsDto;
import com.gtc.model.provider.SubscribeStreamDto;
import com.gtc.model.provider.SubscribeMarketPricesDto;
import com.gtc.provider.service.SubsRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.validation.Validator;

/**
 * Created by Valentyn Berezin on 08.03.18.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubsController extends TextWebSocketHandler {

    private final ObjectMapper mapper;
    private final Validator validator;
    private final SubsRegistry registry;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ProviderSubsDto subs = mapper.readValue(message.asBytes(), ProviderSubsDto.class);
        if (!validator.validate(subs).isEmpty()) {
            throw new IllegalArgumentException("Invalid subs");
        }

        if (subs.getMode() == ProviderSubsDto.Mode.TICKER) {
            registry.subscribeTicker(session,
                    mapper.readValue(message.asBytes(), SubscribeStreamDto.class).getClient());
        } else if (subs.getMode() == ProviderSubsDto.Mode.BOOK) {
            registry.subscribeBook(session,
                    mapper.readValue(message.asBytes(), SubscribeStreamDto.class).getClient());
        } else if (subs.getMode() == ProviderSubsDto.Mode.MARKET_PRICE) {
            SubscribeMarketPricesDto prices = mapper.readValue(message.asBytes(), SubscribeMarketPricesDto.class);
            prices.getTo().forEach(it -> registry.subscribeMarket(session, prices.getFrom(), it));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unsubscribe(session);
    }
}
