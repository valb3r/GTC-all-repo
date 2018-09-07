package com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.impl;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.service.compute.TradeBalanceChange;
import com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.FeeFitted;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Valentyn Berezin on 31.08.18.
 */
class BuyLowSellHighFitterTest extends FitterBase {

    @Spy
    private TradeBalanceChange balanceChange;

    @InjectMocks
    private BuyLowSellHighFitter fitter;

    @Test
    void afterNoGainLoss() {
        OrderBook book = OrderBook.builder().bestSell(1.0).build();

        FeeFitted fitted = fitter.after(book, binance);

        assertThat(fitted.getSellPrice()).isEqualByComparingTo("1.0010011");
        assertThat(fitted.getBuyPrice()).isEqualByComparingTo("1");
        assertThat(fitted.getBuyAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getSellAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getProfitFrom()).isEqualByComparingTo("-0.001");
        assertThat(fitted.getProfitTo()).isEqualByComparingTo("9.8E-8");
    }

    @Test
    void afterGainLoss() {
        OrderBook book = OrderBook.builder().bestSell(1.0).build();
        nnBinance.setFuturePriceGainPct(BigDecimal.ONE);

        FeeFitted fitted = fitter.after(book, binance);

        assertThat(fitted.getSellPrice()).isEqualByComparingTo("1.0110111");
        assertThat(fitted.getBuyPrice()).isEqualByComparingTo("1");
        assertThat(fitted.getBuyAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getSellAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getProfitFrom()).isEqualByComparingTo("-0.001");
        assertThat(fitted.getProfitTo()).isEqualByComparingTo("0.010000088");
    }

    @Test
    void afterNoGainNoLoss() {
        OrderBook book = OrderBook.builder().bestSell(1.0).build();
        binance.setMinOrder(BigDecimal.TEN);
        FeeFitted fitted = fitter.after(book, binance);

        assertThat(fitted.getSellPrice()).isEqualByComparingTo("1.0020031");
        assertThat(fitted.getBuyPrice()).isEqualByComparingTo("1");
        assertThat(fitted.getSellAmount()).isEqualByComparingTo("9.99");
        assertThat(fitted.getBuyAmount()).isEqualByComparingTo("10.00");
        assertThat(fitted.getAmount()).isEqualByComparingTo("9.995");
        assertThat(fitted.getProfitFrom()).isEqualByComparingTo("0.0");
        assertThat(fitted.getProfitTo()).isEqualByComparingTo("9.58E-7");
    }

    @Test
    void afterGainNoLoss() {
        OrderBook book = OrderBook.builder().bestSell(1.0).build();
        binance.setMinOrder(BigDecimal.TEN);
        nnBinance.setFuturePriceGainPct(new BigDecimal("0.1"));

        FeeFitted fitted = fitter.after(book, binance);

        assertThat(fitted.getSellPrice()).isEqualByComparingTo("1.0030051");
        assertThat(fitted.getBuyPrice()).isEqualByComparingTo("1");
        assertThat(fitted.getSellAmount()).isEqualByComparingTo("9.99");
        assertThat(fitted.getBuyAmount()).isEqualByComparingTo("10.00");
        assertThat(fitted.getAmount()).isEqualByComparingTo("9.995");
        assertThat(fitted.getProfitFrom()).isEqualByComparingTo("0");
        assertThat(fitted.getProfitTo()).isEqualByComparingTo("0.010000928");
    }

    @Test
    void beforeNoGain() {
        OrderBook book = OrderBook.builder().bestSell(1.0).build();

        FeeFitted fitted = fitter.before(book, hitbtc);

        assertThat(fitted.getSellPrice()).isEqualByComparingTo("1.002004");
        assertThat(fitted.getBuyPrice()).isEqualByComparingTo("1");
        assertThat(fitted.getSellAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getBuyAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getProfitFrom()).isEqualByComparingTo("0");
        assertThat(fitted.getProfitTo()).isEqualByComparingTo("9.95E-7");
    }

    @Test
    void beforeGain() {
        OrderBook book = OrderBook.builder().bestSell(1.0).build();
        nnHitbtc.setFuturePriceGainPct(new BigDecimal("1"));

        FeeFitted fitted = fitter.before(book, hitbtc);

        assertThat(fitted.getSellPrice()).isEqualByComparingTo("1.012024");
        assertThat(fitted.getBuyPrice()).isEqualByComparingTo("1");
        assertThat(fitted.getSellAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getBuyAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getProfitFrom()).isEqualByComparingTo("0");
        assertThat(fitted.getProfitTo()).isEqualByComparingTo("0.010010975");
    }
}
