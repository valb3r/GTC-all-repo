package com.gtc.tradinggateway.controller;

import com.gtc.tradinggateway.service.SubsRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Created by Valentyn Berezin on 08.03.18.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubsController extends TextWebSocketHandler {

    private final SubsRegistry registry;
    private final MessageRateEqualizer equalizer;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        registry.registerSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        equalizer.addMessage(session, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unregisterSession(session);
    }
}
