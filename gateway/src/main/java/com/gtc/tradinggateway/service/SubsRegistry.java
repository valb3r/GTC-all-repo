package com.gtc.tradinggateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtc.model.gateway.BaseMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Valentyn Berezin on 19.06.18.
 */
@Component
@RequiredArgsConstructor
public class SubsRegistry {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    private final ObjectMapper mapper;

    public void registerSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void unregisterSession(WebSocketSession session) {
        sessions.remove(session);
    }

    @Scheduled(fixedRateString = "${app.schedule.pingMs}")
    public void ping() {
        sessions.forEach(subs -> doSend(subs, new BaseMessage() {

            @Override
            public String type() {
                return "ping";
            }
        }));
    }

    @SneakyThrows
    public void doSend(WebSocketSession session, BaseMessage payload) {
        synchronized (locks.computeIfAbsent(session.getId(), id -> new Object())) {
            payload.setType(payload.type());
            String msg = mapper.writeValueAsString(payload);
            session.sendMessage(new TextMessage(msg));
        }
    }
}
