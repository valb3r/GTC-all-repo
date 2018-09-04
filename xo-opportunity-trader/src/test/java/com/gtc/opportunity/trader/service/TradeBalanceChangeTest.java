package com.gtc.opportunity.trader.service;

import com.google.common.collect.ImmutableList;
import com.gtc.opportunity.trader.BaseMockitoTest;
import com.gtc.opportunity.trader.domain.FeeSystem;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Created by Valentyn Berezin on 03.09.18.
 */
class TradeBalanceChangeTest extends BaseMockitoTest {

    private static final String AMOUNT_ONE = "1.000";
    private static final String AMOUNT_TWO = "2.00";

    @InjectMocks
    private TradeBalanceChange tested;

    @Test
    void computeBeforeSingleBuy() {
        TradeBalanceChange.BalanceChange change = tested
                .compute(
                        ImmutableList.of(buy("0.000890", AMOUNT_ONE)),
                        FeeSystem.FEE_BEFORE,
                        new BigDecimal("0.1")
                );

        assertThat(change.getFrom()).isEqualByComparingTo("1");
        assertThat(change.getTo()).isEqualByComparingTo("-0.00089089");
    }

    @Test
    void computeBeforeSingleSell() {
        TradeBalanceChange.BalanceChange change = tested
                .compute(
                        ImmutableList.of(sell("0.000892", AMOUNT_ONE)),
                        FeeSystem.FEE_BEFORE,
                        new BigDecimal("0.1")
                );

        assertThat(change.getFrom()).isEqualByComparingTo("-1");
        assertThat(change.getTo()).isEqualByComparingTo("0.000891108");
    }

    @Test
    void computeBefore() {
        TradeBalanceChange.BalanceChange change = tested
                .compute(
                        ImmutableList.of(buy("0.000890", AMOUNT_ONE), sell("0.000892", AMOUNT_ONE)),
                        FeeSystem.FEE_BEFORE,
                        new BigDecimal("0.1")
                );

        assertThat(change.getFrom()).isEqualByComparingTo("0");
        assertThat(change.getTo()).isEqualByComparingTo("0.000000218");
    }

    @Test
    void computeAfterSingleBuy() {
        TradeBalanceChange.BalanceChange change = tested
                .compute(
                        ImmutableList.of(buy("0.0008787", AMOUNT_TWO)),
                        FeeSystem.FEE_AFTER,
                        new BigDecimal("0.1")
                );

        assertThat(change.getFrom()).isEqualByComparingTo("1.998");
        assertThat(change.getTo()).isEqualByComparingTo("-0.0017574");
    }

    @Test
    void computeAfterSingleSell() {
        TradeBalanceChange.BalanceChange change = tested
                .compute(
                        ImmutableList.of(sell("0.0008781", AMOUNT_TWO)),
                        FeeSystem.FEE_AFTER,
                        new BigDecimal("0.1")
                );

        assertThat(change.getFrom()).isEqualByComparingTo("-2");
        // NOTE: real value is 0.00175444 due to wallet precision
        assertThat(change.getTo()).isEqualByComparingTo("0.001754443");
    }

    @Test
    void computeAfter() {
        TradeBalanceChange.BalanceChange change = tested
                .compute(
                        ImmutableList.of(buy("0.0008787", AMOUNT_TWO), sell("0.0008781", AMOUNT_TWO)),
                        FeeSystem.FEE_AFTER,
                        new BigDecimal("0.1")
                );

        assertThat(change.getFrom()).isEqualByComparingTo("-0.002");
        // NOTE: real value is 0.00000296 due to wallet precision
        assertThat(change.getTo()).isEqualByComparingTo("-0.000002957");
    }

    private TradeBalanceChange.TradeDesc sell(String price, String amount) {
        return new TradeBalanceChange.TradeDesc(new BigDecimal(price), new BigDecimal(amount), true);
    }

    private TradeBalanceChange.TradeDesc buy(String price, String amount) {
        return new TradeBalanceChange.TradeDesc(new BigDecimal(price), new BigDecimal(amount), false);
    }
}
