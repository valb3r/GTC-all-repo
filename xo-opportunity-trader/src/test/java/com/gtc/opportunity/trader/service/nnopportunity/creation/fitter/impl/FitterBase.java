package com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.impl;

import com.gtc.opportunity.trader.BaseMockitoTest;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.NnConfig;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 31.08.18.
 */
public abstract class FitterBase extends BaseMockitoTest {

    ClientConfig hitbtc;
    ClientConfig binance;
    NnConfig nnHitbtc;
    NnConfig nnBinance;

    @BeforeEach
    public void buildConfigs() {
        hitbtc = ClientConfig.builder()
                .minOrder(BigDecimal.ONE)
                .tradeChargeRatePct(new BigDecimal("0.1"))
                .scalePrice(6)
                .scaleAmount(3)
                .build();
        nnHitbtc = NnConfig.builder().futurePriceGainPct(BigDecimal.ZERO).build();
        hitbtc.setNnConfig(nnHitbtc);

        binance = ClientConfig.builder()
                .minOrder(BigDecimal.ONE)
                .tradeChargeRatePct(new BigDecimal("0.1"))
                .scalePrice(7)
                .scaleAmount(2)
                .build();
        nnBinance = NnConfig.builder().futurePriceGainPct(BigDecimal.ZERO).build();
        binance.setNnConfig(nnBinance);
    }
}
