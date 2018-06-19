package com.gtc.provider.service;

import com.gtc.provider.clients.MarketDto;
import com.gtc.provider.controller.dto.stat.StatDto;
import com.gtc.model.provider.Bid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by Valentyn Berezin on 01.01.18.
 */
@Slf4j
@Service
public class ClientStatProvider {

    private final DeclaredClientsProvider clients;

    public ClientStatProvider(DeclaredClientsProvider clients) {
        this.clients = clients;
    }

    public StatDto calculateStats() {
        StatDto result = new StatDto(new HashMap<>());

        clients.getClientList().forEach(client -> {
            MarketDto market = client.market();
            List<StatDto.ClientStat> stats = new ArrayList<>();
            market.getTicker().forEach((channel, ticker) -> {
                Collection<Bid> bids = market.getMarket().getOrDefault(channel, new ArrayList<>());

                StatDto.ClientStat.Timestamp timestamp = new StatDto.ClientStat.Timestamp(
                        ticker.getTimestamp(),
                        bids.stream().mapToLong(Bid::getTimestamp).min().orElse(0L),
                        bids.stream().mapToLong(Bid::getTimestamp).max().orElse(0L),
                        (long) bids.stream().mapToLong(Bid::getTimestamp).average().orElse(0.0)
                );

                stats.add(
                        new StatDto.ClientStat(
                                "",
                                !client.isDisconnected(),
                                channel,
                                timestamp,
                                bids.size()
                        )
                );
            });

            result.getClients().put(client.name(), stats);

        });

        return result;
    }
}
