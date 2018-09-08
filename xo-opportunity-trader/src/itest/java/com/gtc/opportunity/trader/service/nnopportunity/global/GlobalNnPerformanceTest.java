package com.gtc.opportunity.trader.service.nnopportunity.global;

import com.google.common.base.Joiner;
import com.google.common.io.Resources;
import com.gtc.meta.CurrencyPair;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.command.manage.CancelOrderCommand;
import com.gtc.model.gateway.command.manage.GetOrderCommand;
import com.gtc.model.gateway.command.manage.ListOpenCommand;
import com.gtc.model.gateway.response.manage.GetOrderResponse;
import com.gtc.model.gateway.response.manage.ListOpenOrdersResponse;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.BaseIT;
import com.gtc.opportunity.trader.domain.*;
import com.gtc.opportunity.trader.repository.*;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayResponseListener;
import com.gtc.opportunity.trader.service.nnopportunity.NnDispatcher;
import com.gtc.opportunity.trader.service.nnopportunity.solver.NnSolver;
import com.gtc.opportunity.trader.service.nnopportunity.solver.time.LocalTime;
import com.gtc.opportunity.trader.service.scheduled.trade.management.NnOrderHardCanceller;
import com.gtc.opportunity.trader.service.scheduled.trade.management.NnOrderSoftCanceller;
import com.gtc.opportunity.trader.service.scheduled.trade.management.NnSlaveOrderPusher;
import com.gtc.opportunity.trader.service.scheduled.trade.management.OrderUpdater;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Note: orders are created immediately instead of going one-by-one
 *
 * Test will only start if it sees property GLOBAL_NN_TEST == true.
 * Uses env. vars or (defaults):
 * HISTORY_DIR (/mnt/storage-box/bid/history)
 * CLIENT_NAME (binance)
 * FROM (EOS)
 * TO (BTC)
 * START (2018-07-27T00:00:00)
 * END (2018-07-31T08:00:00)
 * CHARGE_RATE_PCT (0.1)
 * SCALE_AMOUNT (2)
 * SCALE_PRICE(7)
 * MIN_ORDER(1)
 * MAX_ORDER(10)
 * FUTURE_GAIN_PCT (0.2)
 * NOOP_THRESHOLD (1.002)
 * NN_CONFIG (yaml file location - default_nn.yaml)
 * REBUILD_MODEL_EACH_N (10000) - approx 10000/36000 hour
 */
@Slf4j
@ContextConfiguration(classes = {GlobalNnPerformanceTest.RecordedOnInterceptor.class})
public class GlobalNnPerformanceTest extends BaseIT {

    private static final int REQUEST_LAG_N = 20; // imitate network delay ~2s given 100ms data
    private static final int SKIP_PTS_COST_PER_S = 10;
    private static final int LOG_STATS_EACH_N = 10000;

    private static AtomicLong lastBookTimestamp = new AtomicLong();

    private EnvContainer env = new EnvContainer();
    private TestTradeRepository testTradeRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientConfigRepository configRepository;

    @Autowired
    private NnConfigRepository nnConfigRepository;

    @Autowired
    private SoftCancelConfigRepository softCancelConfigRepository;

    @Autowired
    private NnOrderSoftCanceller softCanceller;

    @Autowired
    private NnOrderHardCanceller hardCanceller;

    @Autowired
    private OrderUpdater updater;

    @Autowired
    private NnSlaveOrderPusher pusher;

    @Autowired
    private WsGatewayResponseListener responseListener;

    @Autowired
    private TransactionTemplate template;

    @MockBean
    private LocalTime localTime;

    @MockBean
    private CurrentTimestamp currentTimestamp;

    @SpyBean
    private NnSolver solver;

    @SpyBean
    private NnDispatcher disptacher;

    @MockBean
    private WsGatewayCommander commander;

    @BeforeEach
    void init() {
        System.setProperty("ND4J_FALLBACK", "true");
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        ClientConfig cfg = initClientConfigCache();
        initTradeCreationService();
        initLocalTime();
        testTradeRepository = new TestTradeRepository(cfg, env, env.getClientName(), env.getFrom(), env.getTo());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GLOBAL_NN_TEST", matches = "true")
    void test() {
        long time = System.currentTimeMillis();
        Long bookStart = null;
        AtomicLong pointIndex = new AtomicLong();
        try (HistoryBookReader reader = reader()) {
            while (true) {
                OrderBook book = reader.read();
                if (null == bookStart) {
                    bookStart = book.getMeta().getTimestamp();
                }
                book.getMeta().setTimestamp(book.getMeta().getTimestamp() + time - bookStart);
                lastBookTimestamp.set(book.getMeta().getTimestamp());
                testTradeRepository.acceptOrderBook(book);
                pointIndex.getAndIncrement();
                template.execute(status -> {
                    disptacher.acceptOrderBook(book);
                    printIntermediateStatsIfNeeded(pointIndex.get());
                    generalSchedule(pointIndex.get());
                    cancelOrdersIfNeeded(pointIndex.get());
                    createModelsIfNeeded(pointIndex.get(), reader);
                    return null;
                });
            }
        } catch (NoSuchElementException ex) {
            // NOP
        } catch (Exception ex) {
            log.error("Exception caught", ex);
        } finally {
            testTradeRepository.logStats();
        }
    }

    private void printIntermediateStatsIfNeeded(long pointIndex) {
        if (pointIndex % LOG_STATS_EACH_N != 0) {
            return;
        }

        testTradeRepository.logStats();
    }

    private void createModelsIfNeeded(long pointIndex, HistoryBookReader toSkipOn) {
        if (pointIndex % env.getRebuildModelEachN() != 0) {
            return;
        }
        long st = System.currentTimeMillis();
        solver.createModels();
        long en = System.currentTimeMillis();

        // it is not what happens in reality, since old model will accept point while new is absent
        long skip = (en - st) / 1000L * SKIP_PTS_COST_PER_S;
        for (long i = 0; i < skip; ++i) {
            OrderBook book = toSkipOn.read();
            testTradeRepository.acceptOrderBook(book);
        }
    }

    private void generalSchedule(long pointIndex) {
        if (pointIndex % env.getGeneralScheduleEachN() != 0) {
            return;
        }

        updater.bulkUpdateOrderStatus();
        updater.orderTimeouter();
        updater.stuckUpdateOrderStatus();
        pusher.pushOrders();
    }

    private void cancelOrdersIfNeeded(long pointIndex) {
        if (pointIndex % env.getCheckCancelOrdersEachN() != 0) {
            return;
        }

        hardCanceller.hardCancel();
        softCanceller.softCancel();
    }

    private HistoryBookReader reader() {
        String client = getClientName();
        CurrencyPair pair = new CurrencyPair(env.getFrom(), env.getTo());
        return new HistoryBookReader(env.getHistoryDir(), client, pair, env.getStart(), env.getEnd());
    }

    private String getClientName() {
        return env.getClientName();
    }

    private ClientConfig initClientConfigCache() {
        ClientConfig cfg = template.execute(status -> configRepository.save(initializeBaseReposAndGetClientCfg()));
        template.execute(status -> {
            ClientConfig config = configRepository.findById(cfg.getId()).get();
            nnConfigRepository.save(getNnConfig(config));
            softCancelConfigRepository.save(softCancelConfig(config));
            return null;
        });
        return cfg;
    }

    private ClientConfig initializeBaseReposAndGetClientCfg() {
        Client client = clientRepository.save(
                new Client(env.getClientName(), true, new ArrayList<>(), new ArrayList<>()));
        walletRepository
                .save(Wallet.builder().currency(env.getFrom()).balance(env.getBalFrom()).client(client).build());
        walletRepository.save(Wallet.builder().currency(env.getTo()).balance(env.getBalTo()).client(client).build());
        ClientConfig cfg = ClientConfig.builder()
                .client(client)
                .currency(env.getFrom())
                .currencyTo(env.getTo())
                .scaleAmount(env.getScaleAmount())
                .scalePrice(env.getScalePrice())
                .minOrder(env.getMinOrder())
                .maxOrder(env.getMaxOrder())
                .minOrderInToCurrency(new BigDecimal("0.001"))
                .tradeChargeRatePct(env.getChargeRatePct())
                .build();
        cfg.setEnabled(true);
        cfg.setFeeSystem(env.getFeeSystem());
        return cfg;
    }

    private NnConfig getNnConfig(ClientConfig cfg) {
        return NnConfig.builder()
                .clientCfg(cfg)
                .averageDtSBetweenLabels(new BigDecimal("0.5"))
                .bookTestForOpenPerS(env.getBookTestForOpenPerS())
                .collectNlabeled(env.getCollectNlabellled())
                .futureNwindow(env.getFutureNwindow())
                .futurePriceGainPct(new BigDecimal(env.getFutureGainPct().doubleValue()))
                .networkYamlSpec(env.getNnConfig())
                .noopThreshold(env.getNoopThreshold())
                .nTrainIterations(100)
                .oldThresholdM(Integer.MAX_VALUE)
                .proceedFalsePositive(new BigDecimal("0.3"))
                .trainRelativeSize(new BigDecimal("0.7"))
                .truthThreshold(env.getTruthThreshold())
                .expireOpenH(240)
                .maxSlaveDelayM(30)
                .enabled(true)
                .build();
    }

    private SoftCancelConfig softCancelConfig(ClientConfig cfg) {
        return SoftCancelConfig.builder()
                .clientCfg(cfg)
                .waitM(60)
                .minPriceLossPct(new BigDecimal("0.5"))
                .maxPriceLossPct(new BigDecimal("5"))
                .doneToCancelRatio(BigDecimal.TEN)
                .enabled(true)
                .build();
    }

    // Here comes great simplification - we create orders immediately
    private void initTradeCreationService() {
        doAnswer(invocation -> {
            testTradeRepository.acceptTrade(invocation.getArgument(0), REQUEST_LAG_N);
            return null;
        }).when(commander).createOrder(any(CreateOrderCommand.class));

        doAnswer(invocation -> {
            testTradeRepository.cancelTrade(invocation.getArgument(0));
            return null;
        }).when(commander).cancel(any(CancelOrderCommand.class));

        doAnswer(invocation -> {
            GetOrderResponse response = testTradeRepository.getTrade(invocation.getArgument(0));
            if (null != response) {
                responseListener.byId(response);
            }
            return null;
        }).when(commander).getOrder(any(GetOrderCommand.class));

        doAnswer(invocation -> {
            ListOpenOrdersResponse response = testTradeRepository.listOpen(invocation.getArgument(0));
            if (null != response) {
                responseListener.opened(response);
            }
            return null;
        }).when(commander).listOpenOrders(any(ListOpenCommand.class));
    }

    private void initLocalTime() {
        when(localTime.timestampMs()).thenAnswer(invocation -> lastBookTimestamp.get());
        when(currentTimestamp.dbNow()).thenAnswer(invocation -> lastBookTime());
    }

    private static LocalDateTime lastBookTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(lastBookTimestamp.get()), ZoneOffset.UTC);
    }

    @Aspect
    @Service
    public static class RecordedOnInterceptor {

        @Around("execution(public * com.gtc.opportunity.trader.repository.TradeRepository.save(..))")
        public Object setRecordedOn(ProceedingJoinPoint joinPoint) throws Throwable {
            Trade trade = (Trade) joinPoint.getArgs()[0];
            trade.setRecordedOn(lastBookTime());
            trade.setStatusUpdated(lastBookTime());
            return joinPoint.proceed();
        }
    }

    @Data
    static class EnvContainer {
        // Are set via env:
        private String historyDir = get("HISTORY_DIR", "/mnt/storage-box/bid/history");
        private String clientName = get("CLIENT_NAME", "binance");
        private TradingCurrency from = TradingCurrency.fromCode(get("FROM", "EOS"));
        private TradingCurrency to = TradingCurrency.fromCode(get("TO", "BTC"));

        private LocalDateTime start = LocalDateTime.parse(
                get("START", "2018-07-27T00:00:00"), DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );

        private LocalDateTime end = LocalDateTime.parse(
                get("END", "2018-07-31T08:00:00"), DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );

        private int scaleAmount = Integer.parseInt(get("SCALE_AMOUNT", "2"));
        private int scalePrice = Integer.parseInt(get("SCALE_PRICE", "7"));
        private BigDecimal chargeRatePct = new BigDecimal(get("CHARGE_RATE_PCT", "0.1"));
        private BigDecimal minOrder = new BigDecimal(get("MIN_ORDER", "1"));
        private BigDecimal maxOrder = new BigDecimal(get("MAX_ORDER", "10"));
        private BigDecimal balFrom = new BigDecimal(get("BAL_FROM", "100"));
        private BigDecimal balTo = new BigDecimal(get("BAL_TO", "1"));
        private int rebuildModelEachN = Integer.parseInt(get("REBUILD_MODEL_EACH_N", "10000"));
        private int checkCancelOrdersEachN = Integer.parseInt(get("CANCEL_ORDERS_EACH_N", "1000"));
        private int generalScheduleEachN = Integer.parseInt(get("GENERAL_SCHEDULE_EACH_N", "500"));
        private int collectNlabellled = Integer.parseInt(get("COLLECT_N_LABELED", "1000"));
        private int futureNwindow = Integer.parseInt(get("FUTURE_N_WINDOW", "36000"));
        private String nnConfig = getConfig(get("NN_CONFIG", "nn/default_nn.yaml"));

        private BigDecimal truthThreshold = new BigDecimal(get("TRUTH_THRESHOLD", "0.7"));
        private BigDecimal bookTestForOpenPerS = new BigDecimal(get("BOOK_TEST_FOR_OPEN_S", "0.01"));
        private BigDecimal futureGainPct = new BigDecimal(get("FUTURE_GAIN_PCT", "0.2"));
        private BigDecimal noopThreshold = new BigDecimal(get("NOOP_THRESHOLD", "1.002"));
        private FeeSystem feeSystem = FeeSystem.valueOf(get("FEE_SYSTEM", "FEE_AFTER"));

        @SneakyThrows
        private static String getConfig(String resourcePath) {
            return Joiner.on(System.lineSeparator()).join(
                    Resources.readLines(Resources.getResource(resourcePath), StandardCharsets.UTF_8)
            );
        }

        private static String get(String name, String defaultVal) {
            return System.getenv().getOrDefault(name, defaultVal);
        }
    }
}
