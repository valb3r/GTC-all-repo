package com.gtc.opportunity.trader.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtc.opportunity.trader.config.WsConfig;
import com.gtc.opportunity.trader.repository.NnConfigRepository;
import com.gtc.opportunity.trader.service.nnopportunity.NnDisptacher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Created by Valentyn Berezin on 16.06.18.
 */
@Slf4j
@Service
public class NnWsMarketDataClient extends BaseWsMarketDataClient {

    public NnWsMarketDataClient(
            WsConfig wsConfig,
            ObjectMapper objectMapper,
            NnConfigRepository configs,
            NnDisptacher disptacher) {
        super(
                wsConfig,
                objectMapper,
                () -> configs.findAllActive().stream()
                        .map(it -> it.getClientCfg().getClient().getName())
                        .collect(Collectors.toList()),
                disptacher::acceptOrderBook
        );
    }
}
