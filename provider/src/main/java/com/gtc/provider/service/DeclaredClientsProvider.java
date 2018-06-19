package com.gtc.provider.service;

import com.gtc.provider.clients.WsClient;
import com.gtc.provider.config.ClientsConf;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 01.01.18.
 */
@Component
public class DeclaredClientsProvider {

    @Getter
    private final List<? extends WsClient> clientList;

    public DeclaredClientsProvider(List<? extends WsClient> clients, ClientsConf conf) {
        this.clientList = clients.stream()
                .filter(client -> conf.getActive().contains(client.name()))
                .collect(Collectors.toList());
    }
}
