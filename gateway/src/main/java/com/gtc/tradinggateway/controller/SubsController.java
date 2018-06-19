package com.gtc.tradinggateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.gtc.model.gateway.BaseMessage;
import com.gtc.model.gateway.command.account.GetAllBalancesCommand;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.command.create.MultiOrderCreateCommand;
import com.gtc.model.gateway.command.manage.CancelOrderCommand;
import com.gtc.model.gateway.command.manage.GetOrderCommand;
import com.gtc.model.gateway.command.manage.ListOpenCommand;
import com.gtc.model.gateway.command.withdraw.WithdrawCommand;
import com.gtc.tradinggateway.service.SubsRegistry;
import com.gtc.tradinggateway.service.command.WsCommandHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created by Valentyn Berezin on 08.03.18.
 */
@Slf4j
@Component
public class SubsController extends TextWebSocketHandler {

    private final ObjectMapper mapper;
    private final SubsRegistry registry;
    private final Map<String, BiConsumer<WebSocketSession, TextMessage>> handlers;

    public SubsController(ObjectMapper mapper, SubsRegistry registry, WsCommandHandler handler) {
        this.mapper = mapper;
        this.registry = registry;
        this.handlers = ImmutableMap.<String, BiConsumer<WebSocketSession, TextMessage>>builder()
                .put(GetAllBalancesCommand.TYPE, (s, m) -> handler.getAllBalances(s, read(m, GetAllBalancesCommand.class)))
                .put(CreateOrderCommand.TYPE, (s, m) -> handler.create(s, read(m, CreateOrderCommand.class)))
                .put(MultiOrderCreateCommand.TYPE, (s, m) -> handler.create(s, read(m, MultiOrderCreateCommand.class)))
                .put(GetOrderCommand.TYPE, (s, m) -> handler.get(s, read(m, GetOrderCommand.class)))
                .put(ListOpenCommand.TYPE, (s, m) -> handler.listOpen(s, read(m, ListOpenCommand.class)))
                .put(CancelOrderCommand.TYPE, (s, m) -> handler.cancel(s, read(m, CancelOrderCommand.class)))
                .put(WithdrawCommand.TYPE, (s, m) -> handler.withdraw(s, read(m, WithdrawCommand.class)))
                .build();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        registry.registerSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        BaseMessage subs = mapper.readValue(message.asBytes(), BaseMessage.class);
        BiConsumer<WebSocketSession, TextMessage> handler = handlers.get(subs.getType());
        if (null == handler) {
            throw new IllegalStateException("No handler");
        }

        handler.accept(session, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unregisterSession(session);
    }

    @SneakyThrows
    private <T> T read(TextMessage message, Class<T> clazz) {
        return mapper.readValue(message.asBytes(), clazz);
    }
}
