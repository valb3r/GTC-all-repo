package com.gtc.tradinggateway.config;

import com.gtc.tradinggateway.controller.SubsController;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static com.gtc.tradinggateway.config.Const.Ws.WS_API;


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
        registry.addHandler(subsController, WS_API)
                .setAllowedOrigins("*");
    }
}
