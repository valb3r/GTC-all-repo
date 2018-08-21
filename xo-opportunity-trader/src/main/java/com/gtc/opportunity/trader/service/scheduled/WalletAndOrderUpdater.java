package com.gtc.opportunity.trader.service.scheduled;

import com.gtc.model.gateway.command.account.GetAllBalancesCommand;
import com.gtc.opportunity.trader.repository.ClientRepository;
import com.gtc.opportunity.trader.service.UuidGenerator;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletAndOrderUpdater {

    private static final String WALLET_UPDATE_MS = "#{${app.schedule.wallet.updateS} * 1000}";

    private final ClientRepository clientRepository;
    private final WsGatewayCommander commander;

    @Trace(dispatcher = true)
    @Scheduled(initialDelayString = WALLET_UPDATE_MS, fixedRateString = WALLET_UPDATE_MS)
    @Transactional(readOnly = true)
    public void walletUpdater() {
        clientRepository.findByEnabledTrue().stream()
                .map(it -> GetAllBalancesCommand.builder()
                        .id(UuidGenerator.get())
                        .clientName(it.getName())
                        .build())
                .forEach(commander::getBalances);
    }
}
