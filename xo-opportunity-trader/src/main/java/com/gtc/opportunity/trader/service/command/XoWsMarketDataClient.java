package com.gtc.opportunity.trader.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtc.model.provider.ProviderSubsDto;
import com.gtc.model.provider.SubscribeStreamDto;
import com.gtc.opportunity.trader.config.WsConfig;
import com.gtc.opportunity.trader.repository.ClientRepository;
import com.gtc.opportunity.trader.service.xoopportunity.finder.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 16.06.18.
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "XO_ENABLED", havingValue = "true")
public class XoWsMarketDataClient extends BaseWsProviderClient {

    public XoWsMarketDataClient(
            WsConfig wsConfig,
            ObjectMapper objectMapper,
            ClientRepository clientRepository,
            BookRepository bookRepository) {
        super(
                "xoMarket",
                wsConfig,
                objectMapper,
                () -> clientRepository.findByEnabledTrue().stream()
                        .map(it -> new SubscribeStreamDto(
                                ProviderSubsDto.Mode.BOOK,
                                it.getName()))
                        .collect(Collectors.toList()),
                bookRepository::addOrderBook
        );
    }
}
