package com.gtc.tradinggateway.service.hitbtc;

import com.appunite.websocket.rx.object.ObjectWebSocketSender;
import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.gtc.tradinggateway.BaseMockitoTest;
import com.gtc.tradinggateway.config.HitbtcConfig;
import com.gtc.tradinggateway.meta.PairSymbol;
import com.gtc.tradinggateway.meta.TradingCurrency;
import com.gtc.tradinggateway.service.dto.OrderCreatedDto;
import com.gtc.tradinggateway.service.hitbtc.dto.HitbtcCreateRequestDto;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by mikro on 18.02.2018.
 */
@Slf4j
public class HitbtcWsServiceTest extends BaseMockitoTest {

    private static final TradingCurrency from = TradingCurrency.Bitcoin;
    private static final TradingCurrency to = TradingCurrency.Usd;
    private static final BigDecimal price = BigDecimal.valueOf(0.2);
    private static final BigDecimal amount = BigDecimal.valueOf(0.3);

    @Mock
    private HitbtcConfig cfg;

    @Mock
    private RxObjectEventConnected rx;

    @Mock
    private PairSymbol pair;

    @Mock
    private PairSymbol invertedPair;

    @Mock
    private ObjectWebSocketSender sender;

    @InjectMocks
    private HitbtcWsServiceTestable hitbtcWsService;

    @Captor
    private ArgumentCaptor<HitbtcCreateRequestDto> messageCaptor;

    @BeforeEach
    @SneakyThrows
    public void init() {
        hitbtcWsService.setRxConnected(rx);
        hitbtcWsService.setIsLoggedIn(true);
        when(rx.sender()).thenReturn(sender);
        when(sender.sendObjectMessage(messageCaptor.capture())).thenReturn(true);
        when(pair.getIsInverted()).thenReturn(false);
        when(invertedPair.getIsInverted()).thenReturn(true);
        when(cfg.pairFromCurrencyOrThrow(from, to)).thenReturn(pair);
        when(cfg.pairFromCurrencyOrThrow(to, from)).thenReturn(invertedPair);
    }

    @Test
    public void testCreate() {
        Optional<OrderCreatedDto> id = hitbtcWsService.create("", from, to, amount, price);

        HitbtcCreateRequestDto request = messageCaptor.getValue();
        HitbtcCreateRequestDto.OrderBody params = request.getParams();

        assertThat(request.getMethod()).isEqualTo("newOrder");
        assertThat(params.getSide()).isEqualTo("buy");
        assertThat(params.getPrice()).isEqualTo(price);
        assertThat(params.getQuantity()).isEqualTo(amount);
    }

    @Test
    public void testInvertedCreate() {
        Optional<OrderCreatedDto> id = hitbtcWsService.create("", to, from, amount, price);

        HitbtcCreateRequestDto request = messageCaptor.getValue();
        HitbtcCreateRequestDto.OrderBody params = request.getParams();

        assertThat(request.getMethod()).isEqualTo("newOrder");
        assertThat(params.getSide()).isEqualTo("sell");
        assertThat(params.getPrice()).isEqualTo(BigDecimal.ONE.divide(price, 1, RoundingMode.HALF_EVEN));
        assertThat(params.getQuantity()).isEqualTo(amount.negate().multiply(price).abs());
    }

    public static class HitbtcWsServiceTestable extends HitbtcWsService {

        public HitbtcWsServiceTestable(HitbtcConfig cfg) {
            super(cfg);
        }

        public void setRxConnected(RxObjectEventConnected value) {
            rxConnected.set(value);
        }

        public void setIsLoggedIn(Boolean value) {
            isLoggedIn.set(value);
        }
    }
}
