package com.gtc.opportunity.trader.service.nnopportunity.global;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.gtc.meta.CurrencyPair;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.BaseMockitoTest;
import com.gtc.opportunity.trader.config.CacheConfig;
import com.gtc.opportunity.trader.domain.Client;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.NnConfig;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.repository.*;
import com.gtc.opportunity.trader.service.TradeCreationService;
import com.gtc.opportunity.trader.service.UuidGenerator;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.gtc.opportunity.trader.service.dto.TradeDto;
import com.gtc.opportunity.trader.service.nnopportunity.NnDisptacher;
import com.gtc.opportunity.trader.service.nnopportunity.creation.NnCreateTradesService;
import com.gtc.opportunity.trader.service.nnopportunity.repository.NnDataRepository;
import com.gtc.opportunity.trader.service.nnopportunity.solver.NnAnalyzer;
import com.gtc.opportunity.trader.service.nnopportunity.solver.NnSolver;
import com.gtc.opportunity.trader.service.nnopportunity.solver.model.FeatureMapper;
import com.gtc.opportunity.trader.service.nnopportunity.solver.model.ModelFactory;
import com.gtc.opportunity.trader.service.nnopportunity.solver.time.LocalTime;
import com.gtc.opportunity.trader.service.xoopportunity.creation.ConfigCache;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
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
public class GlobalNnPerformanceTest extends BaseMockitoTest {

    private static final int REQUEST_LAG_N = 20; // imitate network delay ~2s given 100ms data
    private static final int SKIP_PTS_COST_PER_S = 10;
    private static final int LOG_STATS_EACH_N = 10000;

    private EnvContainer env = new EnvContainer();

    private AtomicLong lastBookTimestamp = new AtomicLong();
    private LocalTime localTime;
    private TestTradeRepository testTradeRepository;
    private ConfigCache configs;
    private NnDataRepository repository;
    private FeatureMapper mapper;
    private ModelFactory modelFactory;
    private NnSolver solver;
    private NnCreateTradesService createTradesService;
    private NnAnalyzer nnAnalyzer;
    private NnDisptacher disptacher;

    private WsGatewayCommander commander;
    private TradeCreationService tradeCreationService;

    @BeforeEach
    public void init() {
        System.setProperty("ND4J_FALLBACK", "true");
        initClientConfigCache();
        initLocalTime();
        testTradeRepository = new TestTradeRepository(ImmutableMap.of(getClientName(), getConfig()));
        repository = new NnDataRepository(configs);
        mapper = new FeatureMapper();
        modelFactory = new ModelFactory(localTime, mapper, configs);
        solver = new NnSolver(localTime, configs, modelFactory, repository);
        createTradesService = tradesService();
        nnAnalyzer = new NnAnalyzer(solver, createTradesService);
        disptacher = new NnDisptacher(repository, nnAnalyzer, configs);
    }

    @Test
    public void test() {
        if (!"true".equals(System.getenv("GLOBAL_NN_TEST"))) {
            return;
        }

        long pointIndex = 0;
        try (HistoryBookReader reader = reader()) {
            while (true) {
                OrderBook book = reader.read();
                lastBookTimestamp.set(book.getMeta().getTimestamp());
                testTradeRepository.acceptOrderBook(book);
                pointIndex++;
                disptacher.acceptOrderBook(book);
                printIntermediateStatsIfNeeded(pointIndex);
                createModelsIfNeeded(pointIndex, reader);
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

    private HistoryBookReader reader() {
        String client = getClientName();
        CurrencyPair pair = new CurrencyPair(env.getFrom(), env.getTo());
        return new HistoryBookReader(env.getHistoryDir(), client, pair, env.getStart(), env.getEnd());
    }

    private String getClientName() {
        return env.getClientName();
    }

    private NnCreateTradesService tradesService() {
        commander = mock(WsGatewayCommander.class);
        tradeCreationService = mock(TradeCreationService.class);

        String name = getClientName();
        initTradeCreationService(name);

        return new NnCreateTradesService(commander, tradeCreationService, configs,
                mock(AcceptedNnTradeRepository.class), mock(TradeRepository.class));
    }

    private void initClientConfigCache() {
        ClientConfigRepository cfgRepo = mock(ClientConfigRepository.class);
        NnConfigRepository nnRepo = mock(NnConfigRepository.class);
        XoConfigRepository xoRepo = mock(XoConfigRepository.class);
        configs = new ConfigCache(cfgRepo, nnRepo, xoRepo, new CacheConfig());
        when(cfgRepo.findActiveByKey(env.getClientName(), env.getFrom(), env.getTo()))
                .thenReturn(Optional.of(getConfig()));
        when(nnRepo.findActiveByKey(env.getClientName(), env.getFrom(), env.getTo()))
                .thenReturn(Optional.of(getConfig().getNnConfig()));
    }

    private ClientConfig getConfig() {
        ClientConfig cfg = ClientConfig.builder()
                .client(new Client(env.getClientName(), true, null, null))
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
        cfg.setNnConfig(getNnConfig(cfg));
        return cfg;
    }

    private NnConfig getNnConfig(ClientConfig cfg) {
        return NnConfig.builder()
                .clientCfg(cfg)
                .averageDtSBetweenLabels(new BigDecimal("0.5"))
                .bookTestForOpenPerS(new BigDecimal("0.1"))
                .collectNlabeled(1000)
                .futureNwindow(36000)
                .futurePriceGainPct(new BigDecimal(env.getFutureGainPct().doubleValue()))
                .networkYamlSpec(env.getNnConfig())
                .noopThreshold(env.getNoopThreshold())
                .nTrainIterations(100)
                .oldThresholdM(Integer.MAX_VALUE)
                .proceedFalsePositive(new BigDecimal("0.3"))
                .trainRelativeSize(new BigDecimal("0.7"))
                .truthThreshold(new BigDecimal("0.7"))
                .enabled(true)
                .build();
    }

    // Here comes great simplification - we create orders immediately
    private void initTradeCreationService(String name) {

        when(tradeCreationService.createTradeNoSideValidation(nullable(Trade.class),
                isA(ClientConfig.class), isA(BigDecimal.class), isA(BigDecimal.class), anyBoolean(), anyBoolean())
        ).thenAnswer(inv -> {
            Trade depends = inv.getArgument(0);
            ClientConfig cfg = inv.getArgument(1);
            BigDecimal price = inv.getArgument(2);
            BigDecimal amount = inv.getArgument(3);
            boolean isSell = inv.getArgument(4);

            String id = UuidGenerator.get();

            CreateOrderCommand trade = CreateOrderCommand.builder()
                    .clientName(name)
                    .currencyFrom(cfg.getCurrency().getCode())
                    .currencyTo(cfg.getCurrencyTo().getCode())
                    .price(price)
                    .amount(isSell ? amount.abs().negate() : amount.abs())
                    // id is not unique, actually order is paired via id
                    .id(null != depends ? depends.getId() : id)
                    .orderId(id)
                    .build();

            testTradeRepository.acceptTrade(trade, REQUEST_LAG_N);
            return new TradeDto(
                    Trade.builder()
                            .id(id)
                            .openingPrice(price)
                            .openingAmount(amount)
                            .build(),
                    trade
            );
        });
    }

    private void initLocalTime() {
        localTime = mock(LocalTime.class);
        when(localTime.timestampMs()).thenAnswer(invocation -> lastBookTimestamp.get());
    }

    @Data
    private static class EnvContainer {
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

        private int scaleAmount = Integer.valueOf(get("SCALE_AMOUNT", "2"));
        private int scalePrice = Integer.valueOf(get("SCALE_PRICE", "7"));
        private BigDecimal chargeRatePct = new BigDecimal(get("CHARGE_RATE_PCT", "0.1"));
        private BigDecimal minOrder = new BigDecimal(get("MIN_ORDER", "1"));
        private BigDecimal maxOrder = new BigDecimal(get("MAX_ORDER", "10"));
        private int rebuildModelEachN = Integer.valueOf(get("REBUILD_MODEL_EACH_N", "10000"));
        private String nnConfig = getConfig(get("NN_CONFIG", "nn/default_nn.yaml"));

        private BigDecimal futureGainPct = new BigDecimal(get("FUTURE_GAIN_PCT", "0.2"));
        private BigDecimal noopThreshold = new BigDecimal(get("NOOP_THRESHOLD", "1.002"));

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
