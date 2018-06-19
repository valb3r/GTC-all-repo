package com.gtc.provider.config;

import com.gtc.provider.controller.SubsController;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static com.gtc.provider.config.Const.Ws.ORDER_TICKER_QUEUE;


/**
 * Created by Valentyn Berezin on 09.03.18.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WsConfig implements WebSocketConfigurer {

    private final SubsController subsController;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(subsController, ORDER_TICKER_QUEUE)
                .setAllowedOrigins("*");
    }
}
