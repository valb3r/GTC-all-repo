package com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.impl;

import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.FeeFitted;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Valentyn Berezin on 31.08.18.
 */
class SellHighBuyLowFitterTest extends FitterBase {

    @InjectMocks
    private SellHighBuyLowFitter fitter;

    @Test
    void afterNoGainLoss() {
        OrderBook book = OrderBook.builder().bestBuy(1.0).build();

        FeeFitted fitted = fitter.after(book, binance);

        assertThat(fitted.getBuyPrice()).isEqualByComparingTo("0.999");
        assertThat(fitted.getSellPrice()).isEqualByComparingTo("1");
        assertThat(fitted.getBuyAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getSellAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getProfitFrom()).isEqualByComparingTo("-0.001");
        assertThat(fitted.getProfitTo()).isEqualByComparingTo("0");
    }

    @Test
    void afterGainLoss() {
        OrderBook book = OrderBook.builder().bestBuy(1.0).build();
        nnBinance.setFuturePriceGainPct(BigDecimal.ONE);

        FeeFitted fitted = fitter.after(book, binance);

        assertThat(fitted.getBuyPrice()).isEqualByComparingTo("0.9891089");
        assertThat(fitted.getSellPrice()).isEqualByComparingTo("1");
        assertThat(fitted.getBuyAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getSellAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getProfitFrom()).isEqualByComparingTo("-0.001");
        assertThat(fitted.getProfitTo()).isEqualByComparingTo("0.0098911");
    }

    @Test
    void afterNoGainNoLoss() {
        OrderBook book = OrderBook.builder().bestBuy(1.0).build();
        binance.setMinOrder(BigDecimal.TEN);
        FeeFitted fitted = fitter.after(book, binance);

        assertThat(fitted.getBuyPrice()).isEqualByComparingTo("0.9970059");
        assertThat(fitted.getSellPrice()).isEqualByComparingTo("1");
        assertThat(fitted.getBuyAmount()).isEqualByComparingTo("10.02");
        assertThat(fitted.getSellAmount()).isEqualByComparingTo("10.00");
        assertThat(fitted.getAmount()).isEqualByComparingTo("10.01");
        assertThat(fitted.getProfitFrom()).isEqualByComparingTo("0.00998");
        assertThat(fitted.getProfitTo()).isEqualByComparingTo("8.82E-7");
    }

    @Test
    void afterGainNoLoss() {
        OrderBook book = OrderBook.builder().bestBuy(1.0).build();
        binance.setMinOrder(BigDecimal.TEN);
        nnBinance.setFuturePriceGainPct(new BigDecimal("0.1"));

        FeeFitted fitted = fitter.after(book, binance);

        assertThat(fitted.getBuyPrice()).isEqualByComparingTo("0.9960099");
        assertThat(fitted.getSellPrice()).isEqualByComparingTo("1");
        assertThat(fitted.getBuyAmount()).isEqualByComparingTo("10.02");
        assertThat(fitted.getSellAmount()).isEqualByComparingTo("10.00");
        assertThat(fitted.getAmount()).isEqualByComparingTo("10.01");
        assertThat(fitted.getProfitFrom()).isEqualByComparingTo("0.00998");
        assertThat(fitted.getProfitTo()).isEqualByComparingTo("0.009980802000");
    }

    @Test
    void beforeNoGain() {
        OrderBook book = OrderBook.builder().bestBuy(1.0).build();

        FeeFitted fitted = fitter.before(book, hitbtc);

        assertThat(fitted.getBuyPrice()).isEqualByComparingTo("0.998001");
        assertThat(fitted.getSellPrice()).isEqualByComparingTo("1");
        assertThat(fitted.getBuyAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getSellAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getProfitFrom()).isEqualByComparingTo("0");
        assertThat(fitted.getProfitTo()).isEqualByComparingTo("0");
    }

    @Test
    void beforeGain() {
        OrderBook book = OrderBook.builder().bestBuy(1.0).build();
        nnHitbtc.setFuturePriceGainPct(new BigDecimal("1"));

        FeeFitted fitted = fitter.before(book, hitbtc);

        assertThat(fitted.getBuyPrice()).isEqualByComparingTo("0.988119");
        assertThat(fitted.getSellPrice()).isEqualByComparingTo("1");
        assertThat(fitted.getBuyAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getSellAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getAmount()).isEqualByComparingTo("1");
        assertThat(fitted.getProfitFrom()).isEqualByComparingTo("0");
        assertThat(fitted.getProfitTo()).isEqualByComparingTo("0.00989189189189188");
    }
}
