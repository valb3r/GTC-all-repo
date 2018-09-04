package com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.impl;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.FeeSystem;
import com.gtc.opportunity.trader.service.compute.BalanceChange;
import com.gtc.opportunity.trader.service.compute.TradeBalanceChange;
import com.gtc.opportunity.trader.service.compute.TradeDesc;
import com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.FeeFitted;
import com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.Fitter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 31.08.18.
 */
@Service
@RequiredArgsConstructor
public class BuyLowSellHighFitter implements Fitter {

    private final TradeBalanceChange balanceChange;

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

        BalanceChange delta = change(priceWeBuy, amount, priceWeSell, amount, config, FeeSystem.FEE_BEFORE);
        return new FeeFitted(
                priceWeBuy,
                priceWeSell,
                amount,
                amount,
                amount,
                delta.getFrom(),
                delta.getTo()
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

        BalanceChange delta = change(priceWeBuy, buyAmount, priceWeSell, sellAmount, config, FeeSystem.FEE_AFTER);
        return new FeeFitted(
                priceWeBuy,
                priceWeSell,
                buyAmount,
                sellAmount,
                Util.avg(sellAmount, buyAmount),
                delta.getFrom(),
                delta.getTo()
        );
    }

    private FeeFitted afterLoss(OrderBook book, ClientConfig config) {
        BigDecimal priceWeBuy = Util.ceilPrice(config, book.getBestSell());
        BigDecimal amount = Util.calculateAmount(config, priceWeBuy);
        double charge = Util.computeCharge(config);
        BigDecimal priceWeSell = Util.ceilPrice(config,
                priceWeBuy.doubleValue() / charge * Util.computeGain(config)
        );

        BalanceChange delta = change(priceWeBuy, amount, priceWeSell, amount, config, FeeSystem.FEE_AFTER);
        return new FeeFitted(
                priceWeBuy,
                priceWeSell,
                amount,
                amount,
                amount,
                delta.getFrom(),
                delta.getTo()
        );
    }

    private BalanceChange change(
            BigDecimal buyPrice, BigDecimal buyAmount, BigDecimal sellPrice, BigDecimal sellAmount,
            ClientConfig config, FeeSystem system) {
        return balanceChange.compute(
                system,
                config.getTradeChargeRatePct(),
                new TradeDesc(buyPrice, buyAmount, false),
                new TradeDesc(sellPrice, sellAmount, true)
        );
    }
}
