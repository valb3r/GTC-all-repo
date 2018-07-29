package com.gtc.opportunity.trader.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtc.opportunity.trader.config.WsConfig;
import com.gtc.opportunity.trader.domain.Client;
import com.gtc.opportunity.trader.repository.ClientRepository;
import com.gtc.opportunity.trader.service.opportunity.finder.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 16.06.18.
 */
@Slf4j
@Service
public class XoWsMarketDataClient extends BaseWsMarketDataClient {

    public XoWsMarketDataClient(
            WsConfig wsConfig,
            ObjectMapper objectMapper,
            ClientRepository clientRepository,
            BookRepository bookRepository) {
        super(
                wsConfig,
                objectMapper,
                () -> clientRepository.findByEnabledTrue().stream().map(Client::getName).collect(Collectors.toList()),
                bookRepository::addOrderBook
        );
    }
}
