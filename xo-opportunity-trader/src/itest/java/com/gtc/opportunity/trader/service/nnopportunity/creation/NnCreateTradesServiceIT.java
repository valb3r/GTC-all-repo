package com.gtc.opportunity.trader.service.nnopportunity.creation;

import com.google.common.collect.Lists;
import com.gtc.meta.CurrencyPair;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.provider.ByClientAndCurrency;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.domain.Client;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.domain.Wallet;
import com.gtc.opportunity.trader.repository.*;
import com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy;
import com.gtc.opportunity.trader.service.nnopportunity.repository.StrategyDetails;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Created by Valentyn Berezin on 02.08.18.
 */
public class NnCreateTradesServiceIT extends BaseIT {

    private static final String CLIENT = "binance";
    private static final TradingCurrency FROM = TradingCurrency.EOS;
    private static final TradingCurrency TO = TradingCurrency.Bitcoin;
    private static final BigDecimal TRADE_CHARGE_RATE_PCT = new BigDecimal("0.1");
    private static final BigDecimal FUTURE_GAIN = new BigDecimal("0.2");
    private static final int SCALE_PRICE = 7;
    private static final int SCALE_AMOUNT = 2;
    private static final BigDecimal MIN_ORDER = BigDecimal.ONE;
    private static final BigDecimal MAX_ORDER = BigDecimal.TEN;

    private static final double CONFIDENCE = 0.8;
    private static final int MODEL_AGE_S = 15000;
    private static final int AVG_NOOP_LABEL_AGE_S = 12345;
    private static final int AVG_ACT_LABEL_AGE_S = 12346;

    private static final double BEST_SELL = 0.0011;
    private static final double BEST_BUY = 0.001;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientConfigRepository configRepository;

    @Autowired
    private NnConfigRepository nnConfigRepository;

    @Autowired
    private NnCreateTradesService tradesService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AcceptedNnTradeRepository nnTradeRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Before
    public void init() {
        Client client = clientRepository.save(new Client(CLIENT, true, null, null));
        ClientConfig cfg = ClientConfig.builder()
                .client(client)
                .currency(FROM)
                .currencyTo(TO)
                .tradeChargeRatePct(TRADE_CHARGE_RATE_PCT)
                .minOrder(MIN_ORDER)
                .maxOrder(MAX_ORDER)
                .scalePrice(SCALE_PRICE)
                .scaleAmount(SCALE_AMOUNT)
                .build();

        NnConfig nnCfg = NnConfig.builder()
                .clientCfg(cfg)
                .futurePriceGainPct(FUTURE_GAIN)
                .truthThreshold(new BigDecimal("0.7"))
                .trainRelativeSize(new BigDecimal("0.7"))
                .proceedFalsePositive(new BigDecimal("0.3"))
                .oldThresholdM(100000000)
                .nTrainIterations(100)
                .noopThreshold(new BigDecimal("1.002"))
                .enabled(true)
                .networkYamlSpec("")
                .collectNlabeled(1000)
                .futureNwindow(36000)
                .bookTestForOpenPerS(BigDecimal.ONE)
                .averageDtSBetweenLabels(BigDecimal.ONE)
                .build();

        cfg.setNnConfig(nnCfg);

        nnConfigRepository.save(nnCfg);
        configRepository.save(cfg);

        walletRepository.save(Wallet.builder()
                .client(client)
                .currency(FROM)
                .balance(BigDecimal.TEN)
                .build());

        walletRepository.save(Wallet.builder()
                .client(client)
                .currency(TO)
                .balance(BigDecimal.TEN)
                .build());
    }

    @Test
    @Transactional
    public void testTradeCreation() {
        tradesService.create(
                new StrategyDetails(
                        Strategy.BUY_LOW_SELL_HIGH,
                        CONFIDENCE, MODEL_AGE_S,
                        AVG_NOOP_LABEL_AGE_S,
                        AVG_ACT_LABEL_AGE_S),
                OrderBook.builder()
                        .meta(new ByClientAndCurrency(CLIENT, new CurrencyPair(FROM, TO)))
                        .bestBuy(BEST_BUY)
                        .bestSell(BEST_SELL)
                    .build()
        );

        assertThat(Lists.newArrayList(nnTradeRepository.findAll()).size()).isEqualTo(1);
        assertThat(Lists.newArrayList(tradeRepository.findAll()).size()).isEqualTo(2);
    }
}
