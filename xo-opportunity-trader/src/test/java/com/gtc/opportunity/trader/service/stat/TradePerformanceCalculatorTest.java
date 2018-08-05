package com.gtc.opportunity.trader.service.stat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.BaseMockitoTest;
import com.gtc.opportunity.trader.domain.*;
import com.gtc.opportunity.trader.repository.CryptoPricingRepository;
import com.gtc.opportunity.trader.service.opportunity.creation.ConfigCache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by Valentyn Berezin on 05.08.18.
 */
public class TradePerformanceCalculatorTest extends BaseMockitoTest {

    private static final String CLIENT_NAME = "binance";
    private static final TradingCurrency FROM = TradingCurrency.Bitcoin;
    private static final TradingCurrency TO = TradingCurrency.Usd;
    private static final BigDecimal LESS = new BigDecimal("10000");
    private static final BigDecimal MORE = new BigDecimal("15000");

    private static final Client CLIENT = new Client(CLIENT_NAME, true, null, null);

    @Mock
    private CryptoPricingRepository pricingRepository;

    @Mock
    private ConfigCache configCache;

    @InjectMocks
    private TradePerformanceCalculator calculator;

    @Before
    public void init() {
        when(pricingRepository.priceList()).thenReturn(
                ImmutableMap.of(
                        FROM, CryptoPricing.builder().currency(FROM).priceBtc(BigDecimal.ONE).build(),
                        TO, CryptoPricing.builder().currency(TO).priceBtc(new BigDecimal("0.0001")).build()
                )
        );

        when(configCache.getClientCfg(CLIENT_NAME, FROM, TO))
                .thenReturn(Optional.of(ClientConfig.builder().tradeChargeRatePct(BigDecimal.TEN).build()));
    }

    @Test
    public void calculateOnGroupedByPairSellBuy() {
        TradePerformanceCalculator.Performance performance = calculator.calculateOnGroupedByPair(
                ImmutableList.of(
                        common().openingAmount(BigDecimal.ONE).openingPrice(MORE).isSell(true).build(),
                        common().openingAmount(BigDecimal.ONE).openingPrice(LESS).isSell(false).build()
                ),
                Trade::getId
        );

        assertGain(performance);
    }

    @Test
    public void calculateOnGroupedByPairBuySell() {
        TradePerformanceCalculator.Performance performance = calculator.calculateOnGroupedByPair(
                ImmutableList.of(
                        common().openingPrice(LESS).isSell(false).build(),
                        common().openingAmount(BigDecimal.ONE).openingPrice(MORE).isSell(true).build()
                ),
                Trade::getId
        );

        assertGain(performance);
    }

    private static Trade.TradeBuilder common() {
        LocalDateTime recorded = LocalDateTime.now();
        LocalDateTime closed = recorded.plusSeconds(1);
        return Trade.builder().client(CLIENT).currencyFrom(FROM).currencyTo(TO).id("1").status(TradeStatus.CLOSED)
                .recordedOn(recorded)
                .statusUpdated(closed)
                .openingAmount(BigDecimal.ONE);
    }

    private void assertGain(TradePerformanceCalculator.Performance performance) {
        assertThat(performance.getTotal()).isEqualByComparingTo("2");
        assertThat(performance.getInErrors()).isEqualByComparingTo("0");
        assertThat(performance.getInOrders()).isEqualByComparingTo("0");
        assertThat(performance.getExpectedProfitBtc()).isEqualByComparingTo("0.25");
        assertThat(performance.getClaimedProfitBtc()).isEqualByComparingTo("0.25");
        assertThat(performance.getLatestTimeToCloseS()).isEqualTo(1);
    }
}
