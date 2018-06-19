package com.gtc.tradinggateway.service.hitbtc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.tradinggateway.BaseMockitoTest;
import com.gtc.tradinggateway.config.ConfigFactory;
import com.gtc.tradinggateway.config.HitbtcConfig;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.hitbtc.dto.HitbtcBalanceItemDto;
import com.gtc.tradinggateway.service.hitbtc.dto.HitbtcOrderGetDto;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by mikro on 18.02.2018.
 */
public class HitbtcRestServiceTest extends BaseMockitoTest {

    private static final String ASSIGNED_ID = "1234";
    private static final String SYMBOL = "BTC";
    private static final String ID = SYMBOL + "." + ASSIGNED_ID;
    private static final String BASE = "base";

    @Captor
    private ArgumentCaptor<HttpEntity> entity;

    @Captor
    private ArgumentCaptor<String> requestCaptor;

    @Mock
    private HitbtcConfig cfg;

    @Mock
    private HitbtcEncryptionService signer;

    @Mock
    private HttpHeaders headers;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HitbtcOrderGetDto getOrderDto;

    @Mock
    private ConfigFactory configFactory;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private HitbtcRestService hitbtcRestService;

    @Before
    public void init() {
        when(cfg.getRestTemplate()).thenReturn(restTemplate);
        when(cfg.getRestBase()).thenReturn(BASE);
        when(cfg.getMapper()).thenReturn(objectMapper);
        when(signer.restHeaders()).thenReturn(headers);
    }

    @Test
    public void testGetOrder() {
        OrderDto expectedOrder = mock(OrderDto.class);
        when(getOrderDto.mapTo()).thenReturn(expectedOrder);
        when(restTemplate.exchange(
                requestCaptor.capture(),
                eq(HttpMethod.GET),
                entity.capture(),
                eq(HitbtcOrderGetDto.class)

        )).thenReturn(new ResponseEntity<>(getOrderDto, HttpStatus.OK));

        Optional<OrderDto> result = hitbtcRestService.get(ID);

        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(expectedOrder);
        assertThat(requestCaptor.getValue()).isEqualTo(BASE + "/order/" + ID);
    }

    @Test
    public void testGetOpenOrders() {
        OrderDto expectedOrder = mock(OrderDto.class);
        HitbtcOrderGetDto[] response = new HitbtcOrderGetDto[]{getOrderDto};
        when(getOrderDto.mapTo()).thenReturn(expectedOrder);
        when(restTemplate.exchange(
                requestCaptor.capture(),
                eq(HttpMethod.GET),
                entity.capture(),
                eq(HitbtcOrderGetDto[].class)

        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        List<OrderDto> results = hitbtcRestService.getOpen(TradingCurrency.Bitcoin, TradingCurrency.Usd);

        assertThat(results.get(0)).isEqualTo(expectedOrder);
        assertThat(requestCaptor.getValue()).isEqualTo(BASE + "/order/");
    }

    @Test
    public void testCancelOrder() {
        when(restTemplate.exchange(
                requestCaptor.capture(),
                eq(HttpMethod.DELETE),
                entity.capture(),
                eq(Object.class)

        )).thenReturn(new ResponseEntity<>(new Object(), HttpStatus.OK));

        hitbtcRestService.cancel(ID);

        assertThat(requestCaptor.getValue()).isEqualTo(BASE + "/order/" + ID);
    }

    @Test
    public void testGetBalances() {
        BigDecimal amount = BigDecimal.valueOf(0.2);
        TradingCurrency currency = TradingCurrency.Bitcoin;
        HitbtcBalanceItemDto responseItem = mock(HitbtcBalanceItemDto.class);
        HitbtcBalanceItemDto[] response = new HitbtcBalanceItemDto[]{responseItem};
        when(responseItem.getCurrency()).thenReturn(SYMBOL);
        when(responseItem.getAvailable()).thenReturn(amount);
        when(restTemplate.exchange(
                requestCaptor.capture(),
                eq(HttpMethod.GET),
                entity.capture(),
                eq(HitbtcBalanceItemDto[].class)

        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        Map<TradingCurrency, BigDecimal> results = hitbtcRestService.balances();

        assertThat(requestCaptor.getValue()).isEqualTo(BASE + "/trading/balance");
        assertThat(results.get(currency)).isEqualTo(amount);
    }

    @Test
    public void testWithdraw() {
        TradingCurrency currency = TradingCurrency.Bitcoin;
        BigDecimal amount = BigDecimal.valueOf(0.2);
        String destination = "0x00";

        when(restTemplate.exchange(
                requestCaptor.capture(),
                eq(HttpMethod.POST),
                entity.capture(),
                eq(Object.class)

        )).thenReturn(new ResponseEntity<>(new Object(), HttpStatus.OK));

        hitbtcRestService.withdraw(currency, amount, destination);

        assertThat(requestCaptor.getValue()).isEqualTo(BASE + "/account/crypto/withdraw");
    }
}
