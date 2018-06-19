package com.gtc.opportunity.trader.service.trade.management;

import com.gtc.meta.TradingCurrency;
import com.gtc.model.gateway.response.account.GetAllBalancesResponse;
import com.gtc.opportunity.trader.domain.Client;
import com.gtc.opportunity.trader.domain.Wallet;
import com.gtc.opportunity.trader.repository.ClientRepository;
import com.gtc.opportunity.trader.repository.WalletRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Valentyn Berezin on 06.03.18.
 */
@Service
@RequiredArgsConstructor
public class WalletEsbEventHandler {

    private final ClientRepository clientRepository;
    private final WalletRepository walletRepository;
    private final CurrentTimestamp currentTimestamp;

    @Transactional
    public void updateWallet(GetAllBalancesResponse response) {
        Client client = clientRepository.findById(response.getClientName())
                .orElseThrow(() -> new IllegalStateException("No client"));
        if (!client.isEnabled()) {
            return;
        }

        response.getBalances().forEach((currCode, amount) -> {
            TradingCurrency currency =  TradingCurrency.fromCode(currCode);
            Wallet wallet = walletRepository.findByClientAndCurrency(client, currency)
                    .orElseGet(() -> Wallet.builder()
                            .client(client)
                            .currency(currency)
                            .build()
                    );

            wallet.setStatusUpdated(currentTimestamp.dbNow());
            wallet.setBalance(amount);
            walletRepository.save(wallet);
        });
    }
}
