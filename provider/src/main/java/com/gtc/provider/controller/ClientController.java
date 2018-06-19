package com.gtc.provider.controller;

import com.gtc.provider.clients.WsClient;
import com.gtc.provider.service.DeclaredClientsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

import static com.gtc.provider.config.Const.Rest.CLIENT;

/**
 * Created by Valentyn Berezin on 15.06.18.
 */
@RestController
@RequestMapping(path = CLIENT)
@RequiredArgsConstructor
public class ClientController {

    private final DeclaredClientsProvider clientsProvider;

    @GetMapping
    public Set<String> getClients() {
        return clientsProvider.getClientList().stream().map(WsClient::name).collect(Collectors.toSet());
    }
}
