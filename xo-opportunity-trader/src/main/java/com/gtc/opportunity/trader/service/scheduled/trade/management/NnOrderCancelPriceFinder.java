package com.gtc.opportunity.trader.service.scheduled.trade.management;

import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.SoftCancel;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.repository.WalletRepository;
import com.gtc.opportunity.trader.service.LatestPrices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 03.09.18.
 */
@Service
@RequiredArgsConstructor
class NnOrderCancelPriceFinder {

    private final WalletRepository walletRepository;
    private final LatestPrices prices;

    @Transactional
    public BigDecimal findLossPrice(ClientConfig cfg, SoftCancel cancelCfg, Trade trade) {
        return trade.isSell() ? sellPrice(cfg, cancelCfg, trade) : buyPrice(cfg, cancelCfg, trade);
    }

    private BigDecimal sellPrice(ClientConfig cfg, SoftCancel cancelCfg, Trade trade) {
        BigDecimal price = prices.bestSell(trade.getClient().getName(), trade.getCurrencyFrom(), trade.getCurrencyTo());
        return price;
    }

    private BigDecimal buyPrice(ClientConfig cfg, SoftCancel cancelCfg, Trade trade) {
        BigDecimal price = prices.bestBuy(trade.getClient().getName(), trade.getCurrencyFrom(), trade.getCurrencyTo());
        return price;
    }
}
