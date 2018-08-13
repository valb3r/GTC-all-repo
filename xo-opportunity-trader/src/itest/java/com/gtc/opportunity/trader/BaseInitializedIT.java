package com.gtc.opportunity.trader;

import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.Client;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.domain.Wallet;
import com.gtc.opportunity.trader.repository.ClientConfigRepository;
import com.gtc.opportunity.trader.repository.ClientRepository;
import com.gtc.opportunity.trader.repository.NnConfigRepository;
import com.gtc.opportunity.trader.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Base initialized IT. Has binance client EOS/BTC pair (NN) and relevant wallets.
 */
@Transactional
public abstract class BaseInitializedIT extends BaseIT {

    protected static final String CLIENT = "binance";
    protected static final TradingCurrency FROM = TradingCurrency.EOS;
    protected static final TradingCurrency TO = TradingCurrency.Bitcoin;
    protected static final BigDecimal TRADE_CHARGE_RATE_PCT = new BigDecimal("0.2");
    protected static final BigDecimal FUTURE_GAIN = new BigDecimal("0.2");
    protected static final int SCALE_PRICE = 7;
    protected static final int SCALE_AMOUNT = 2;
    protected static final BigDecimal MIN_ORDER = BigDecimal.ONE;
    protected static final BigDecimal MAX_ORDER = BigDecimal.TEN;
    protected static final BigDecimal WALLET_BAL = new BigDecimal("10.1");

    @Autowired
    protected ClientRepository clientRepository;

    @Autowired
    protected ClientConfigRepository configRepository;

    @Autowired
    protected NnConfigRepository nnConfigRepository;

    @Autowired
    protected WalletRepository walletRepository;

    protected Client createdClient;
    protected ClientConfig createdConfig;
    protected NnConfig createdNnConfig;
    protected Wallet walletFrom;
    protected Wallet walletTo;

    @BeforeTransaction
    public void init() {
        createdClient = clientRepository.save(new Client(CLIENT, true, null, null));
        createdConfig = ClientConfig.builder()
                .client(createdClient)
                .currency(FROM)
                .currencyTo(TO)
                .tradeChargeRatePct(TRADE_CHARGE_RATE_PCT)
                .minOrder(MIN_ORDER)
                .maxOrder(MAX_ORDER)
                .scalePrice(SCALE_PRICE)
                .scaleAmount(SCALE_AMOUNT)
                .build();

        createdNnConfig = NnConfig.builder()
                .clientCfg(createdConfig)
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
                .maxSlaveDelayM(600)
                .bookTestForOpenPerS(BigDecimal.ONE)
                .averageDtSBetweenLabels(BigDecimal.ONE)
                .build();

        createdConfig.setNnConfig(createdNnConfig);

        nnConfigRepository.save(createdNnConfig);
        configRepository.save(createdConfig);

        walletFrom = walletRepository.save(Wallet.builder()
                .client(createdClient)
                .currency(FROM)
                .balance(WALLET_BAL)
                .build());

        walletTo = walletRepository.save(Wallet.builder()
                .client(createdClient)
                .currency(TO)
                .balance(WALLET_BAL)
                .build());
    }
}
