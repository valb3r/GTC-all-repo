package com.gtc.opportunity.trader.service;

import com.gtc.meta.TradingCurrency;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.opportunity.trader.BaseMockitoTest;
import com.gtc.opportunity.trader.domain.Client;
import com.gtc.opportunity.trader.domain.Trade;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.validation.Validator;
import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Created by Valentyn Berezin on 15.08.18.
 */
class TradeCreationServiceTest extends BaseMockitoTest {

    @Mock
    private Validator validator;

    @InjectMocks
    private TradeCreationService service;

    @Test
    void mapsAsPlainString() {
        CreateOrderCommand command = service.map(Trade.builder()
                .client(new Client())
                .currencyFrom(TradingCurrency.Monero)
                .currencyTo(TradingCurrency.Bitcoin)
                .openingPrice(new BigDecimal("10.000"))
                .openingAmount(new BigDecimal("2E+1"))
                .build());

        assertThat(command.getPrice()).isEqualTo("10");
        assertThat(command.getAmount()).isEqualTo("20");
    }
}
