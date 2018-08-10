package com.gtc.tradinggateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableMap;
import com.gtc.model.gateway.BaseMessage;
import com.gtc.model.gateway.command.account.GetAllBalancesCommand;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.command.create.MultiOrderCreateCommand;
import com.gtc.model.gateway.command.manage.CancelOrderCommand;
import com.gtc.model.gateway.command.manage.GetOrderCommand;
import com.gtc.model.gateway.command.manage.ListOpenCommand;
import com.gtc.model.gateway.command.withdraw.WithdrawCommand;
import com.gtc.tradinggateway.config.RateEqualizerConf;
import com.gtc.tradinggateway.service.command.WsCommandHandler;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Created by Valentyn Berezin on 10.08.18.
 */
@Service
public class MessageRateEqualizer {

    private final Map<String, Queue<MessageAndSession>> messagesByClient = new ConcurrentHashMap<>();

    private final RateEqualizerConf equalizerConf;
    private final ObjectMapper mapper;
    private final Map<String, BiConsumer<WebSocketSession, TextMessage>> handlers;

    public MessageRateEqualizer(RateEqualizerConf equalizerConf, ObjectMapper mapper, WsCommandHandler handler) {
        this.equalizerConf = equalizerConf;
        this.mapper = mapper;
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

    @SneakyThrows
    void addMessage(WebSocketSession session, TextMessage message) {
        BaseMessage subs = mapper.readValue(message.asBytes(), BaseMessage.class);
        BiConsumer<WebSocketSession, TextMessage> handler = handlers.get(subs.getType());
        if (null == handler) {
            throw new IllegalStateException("No handler");
        }

        messagesByClient
                .computeIfAbsent(key(subs), id -> EvictingQueue.create(equalizerConf.getQueueCapacity()))
                .add(new MessageAndSession(session, message, handler));
    }

    @Scheduled(fixedDelayString = "#{${app.rate-equalizer.requestsPerSec} * 1000}")
    public void processMessages() {
        messagesByClient.forEach((k, msg) -> doProcessMessage(msg.poll()));
    }

    @SneakyThrows
    private void doProcessMessage(MessageAndSession msg) {
        if (null == msg || !msg.getSession().isOpen()) {
            return;
        }

        msg.execute();
    }

    @SneakyThrows
    private <T> T read(TextMessage message, Class<T> clazz) {
        return mapper.readValue(message.asBytes(), clazz);
    }

    private static String key(BaseMessage baseMessage) {
        return baseMessage.getClientName();
    }

    @Data
    private static class MessageAndSession {

        private final WebSocketSession session;
        private final TextMessage message;
        private final BiConsumer<WebSocketSession, TextMessage> handler;

        void execute() {
            handler.accept(session, message);
        }
    }
}
