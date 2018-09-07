package com.gtc.opportunity.trader.service.scheduled.trade.management;

import com.gtc.opportunity.trader.domain.SoftCancelConfig;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.service.LatestMarketPrices;
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

    private final LatestMarketPrices marketPrices;

    @Transactional
    public BigDecimal findSuitableLossPrice(SoftCancelConfig cancelCfg, Trade trade) {
        BigDecimal price = trade.isSell() ? weSellPrice(trade) : weBuyPrice(trade);
        if (!checkPrice(price, cancelCfg, trade)) {
            return null;
        }

        if (!checkDoneToCancel(cancelCfg)) {
            return null;
        }

        return price;
    }

    private BigDecimal weSellPrice(Trade trade) {
        return marketPrices.bestBuy(trade.getClient().getName(), trade.getCurrencyFrom(), trade.getCurrencyTo());
    }

    private BigDecimal weBuyPrice(Trade trade) {
        return marketPrices.bestSell(trade.getClient().getName(), trade.getCurrencyFrom(), trade.getCurrencyTo());
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
        if (null == cancelCfg.getCancel()) {
            return false;
        }

        int cancel = cancelCfg.getCancel().getCancelled() + 1;
        int done = cancelCfg.getCancel().getDone();
        return ((double) done / cancel) >= cancelCfg.getDoneToCancelRatio().doubleValue();
    }
}
