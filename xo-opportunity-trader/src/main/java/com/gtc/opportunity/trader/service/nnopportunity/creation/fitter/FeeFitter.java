package com.gtc.opportunity.trader.service.nnopportunity.creation.fitter;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.FeeSystem;
import com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.impl.BuyLowSellHighFitter;
import com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.impl.SellHighBuyLowFitter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by Valentyn Berezin on 31.08.18.
 */
@Service
@RequiredArgsConstructor
public class FeeFitter {

    private final BuyLowSellHighFitter buyLowSellHighFitter;
    private final SellHighBuyLowFitter sellHighBuyLowFitter;

    public FeeFitted sellHighBuyLow(OrderBook book, ClientConfig config) {
        return handle(sellHighBuyLowFitter, book, config);
    }

    public FeeFitted buyLowSellHigh(OrderBook book, ClientConfig config) {
        return handle(buyLowSellHighFitter, book, config);
    }

    private FeeFitted handle(Fitter fitter, OrderBook book, ClientConfig config) {
        if (config.getFeeSystem() == FeeSystem.FEE_AFTER) {
            return fitter.after(book, config);
        } else if (config.getFeeSystem() == FeeSystem.FEE_BEFORE) {
            return fitter.before(book, config);
        }

        throw new IllegalStateException("No fitter");
    }
}
