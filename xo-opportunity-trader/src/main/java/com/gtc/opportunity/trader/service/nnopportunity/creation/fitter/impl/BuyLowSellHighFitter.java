package com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.impl;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.FeeFitted;
import com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.Fitter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 31.08.18.
 */
@Service
public class BuyLowSellHighFitter implements Fitter {

    @Override
    public FeeFitted after(OrderBook book, ClientConfig config) {
        if (Util.canUseNoLoss(config)) {
            return afterNoLoss(book, config);
        }

        return afterLoss(book, config);
    }

    @Override
    public FeeFitted before(OrderBook book, ClientConfig config) {
        BigDecimal priceWeBuy = Util.ceilPrice(config, book.getBestSell());
        BigDecimal amount = Util.calculateAmount(config, priceWeBuy);
        double charge = Util.computeCharge(config);
        BigDecimal priceWeSell = Util.ceilPrice(config,
                priceWeBuy.doubleValue() / charge / charge * Util.computeGain(config)
        );

        return new FeeFitted(
                priceWeBuy,
                priceWeSell,
                amount,
                amount,
                amount,
                BigDecimal.ZERO,
                BigDecimal.valueOf(
                        amount.doubleValue() * (priceWeSell.doubleValue() * charge - priceWeBuy.doubleValue() / charge)
                )
        );
    }

    private FeeFitted afterNoLoss(OrderBook book, ClientConfig config) {
        BigDecimal priceWeBuy = Util.ceilPrice(config, book.getBestSell());
        BigDecimal buyAmount = Util.calculateAmount(config, priceWeBuy);
        double charge = Util.computeCharge(config);
        BigDecimal sellAmount = Util.floorAmount(config, buyAmount.doubleValue() * charge);
        BigDecimal priceWeSell = Util.ceilPrice(config,
                priceWeBuy.doubleValue() * buyAmount.doubleValue() / sellAmount.doubleValue() / charge
                        * Util.computeGain(config)
        );

        return new FeeFitted(
                priceWeBuy,
                priceWeSell,
                buyAmount,
                sellAmount,
                Util.avg(sellAmount, buyAmount),
                buyAmount.multiply(BigDecimal.valueOf(charge)).subtract(sellAmount),
                sellAmount.multiply(priceWeSell).multiply(BigDecimal.valueOf(charge))
                        .subtract(buyAmount.multiply(priceWeBuy))
        );
    }

    private FeeFitted afterLoss(OrderBook book, ClientConfig config) {
        BigDecimal priceWeBuy = Util.ceilPrice(config, book.getBestSell());
        BigDecimal amount = Util.calculateAmount(config, priceWeBuy);
        double charge = Util.computeCharge(config);
        BigDecimal priceWeSell = Util.ceilPrice(config,
                priceWeBuy.doubleValue() / charge * Util.computeGain(config)
        );

        return new FeeFitted(
                priceWeBuy,
                priceWeSell,
                amount,
                amount,
                amount,
                amount.multiply(BigDecimal.valueOf(charge)).subtract(amount),
                amount.multiply(priceWeSell).multiply(BigDecimal.valueOf(charge))
                        .subtract(amount.multiply(priceWeBuy)));
    }
}
