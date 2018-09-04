package com.gtc.opportunity.trader.service.scheduled.trade.management;

import com.gtc.opportunity.trader.domain.SoftCancelConfig;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.service.LatestPrices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Created by Valentyn Berezin on 03.09.18.
 */
@Service
@RequiredArgsConstructor
class NnOrderCancelPriceFinder {

    private final LatestPrices prices;

    @Transactional
    public BigDecimal findSuitableLossPrice(SoftCancelConfig cancelCfg, Trade trade) {
        BigDecimal price = trade.isSell() ? sellPrice(trade) : buyPrice(trade);
        if (!checkPrice(price, cancelCfg, trade)) {
            return null;
        }

        if (!checkDoneToCancel(cancelCfg)) {
            return null;
        }

        return price;
    }

    private BigDecimal sellPrice(Trade trade) {
        return prices.bestSell(trade.getClient().getName(), trade.getCurrencyFrom(), trade.getCurrencyTo());
    }

    private BigDecimal buyPrice(Trade trade) {
        return prices.bestBuy(trade.getClient().getName(), trade.getCurrencyFrom(), trade.getCurrencyTo());
    }

    private boolean checkPrice(BigDecimal proposedPrice, SoftCancelConfig cancel, Trade trade) {
        BigDecimal lossPct = proposedPrice
                .divide(trade.getPrice(), MathContext.DECIMAL128)
                .subtract(BigDecimal.ONE)
                .movePointRight(2)
                .abs();

        return lossPct.compareTo(cancel.getMinPriceLossPct()) >= 0
                && lossPct.compareTo(cancel.getMaxPriceLossPct()) <= 0;
    }

    private boolean checkDoneToCancel(SoftCancelConfig cancelCfg) {
        int cancel = cancelCfg.getCancel().getCancelled() + 1;
        int done = cancelCfg.getCancel().getDone();
        return ((double) done / cancel) >= cancelCfg.getDoneToCancelRatio().doubleValue();
    }
}
