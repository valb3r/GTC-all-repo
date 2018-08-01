package com.gtc.opportunity.trader.service.nnopportunity.global;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.gtc.meta.CurrencyPair;
import com.gtc.meta.TradingCurrency;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.command.create.MultiOrderCreateCommand;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.BaseMockitoTest;
import com.gtc.opportunity.trader.config.NnConfig;
import com.gtc.opportunity.trader.domain.Client;
import com.gtc.opportunity.trader.domain.ClientConfig;
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
import com.gtc.opportunity.trader.service.opportunity.common.TradeCreationService;
import com.gtc.opportunity.trader.service.opportunity.creation.ClientConfigCache;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * Created by Valentyn Berezin on 31.07.18.
 */
@Slf4j
public class GlobalNnPerformanceTest extends BaseMockitoTest {

    private static final String HISTORY_DIR = "/mnt/storage-box/bid/history";
    private static final LocalDateTime FROM = LocalDateTime.of(2018, 7, 27, 0, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2018, 7, 31, 8, 0, 0);

    // This one is part of config
    private static final int REBUILD_MODEL_EACH_N = 10000;

    private static final int SKIP_PTS_COST_PER_S = 10;
    private static final int LOG_STATS_EACH_N = 10000;

    private TestTradeRepository testTradeRepository;

    private NnConfig config;
    private NnDataRepository repository;
    private FeatureMapper mapper;
    private ModelFactory modelFactory;
    private NnSolver solver;
    private NnCreateTradesService createTradesService;
    private NnAnalyzer nnAnalyzer;
    private NnDisptacher disptacher;

    private WsGatewayCommander commander;
    private TradeCreationService tradeCreationService;
    private ClientConfigCache clientConfigCache;

    @Before
    public void init() {
        System.setProperty("ND4J_FALLBACK", "true");
        buildConfig();
        testTradeRepository = new TestTradeRepository(ImmutableMap.of(getClientName(), getConfig(getClientName())));
        repository = new NnDataRepository(config);
        mapper = new FeatureMapper();
        modelFactory = new ModelFactory(config, mapper);
        solver = new NnSolver(config, modelFactory, repository);
        createTradesService = tradesService();
        nnAnalyzer = new NnAnalyzer(solver, createTradesService);
        disptacher = new NnDisptacher(repository, nnAnalyzer, config);
    }

    @Test
    public void test() {
        long pointIndex = 0;
        try (HistoryBookReader reader = reader()) {
            while (true) {
                OrderBook book = reader.read();
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
        if (pointIndex % REBUILD_MODEL_EACH_N != 0) {
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
        CurrencyPair pair = Iterables.getFirst(config.getEnabledOn().get(client), null);
        return new HistoryBookReader(HISTORY_DIR, client, pair, FROM, TO);
    }

    private String getClientName() {
        return Iterables.getFirst(config.getEnabledOn().keySet(), null);
    }

    private void buildConfig() {
        config = new NnConfig();
        config.setEnabledOn(ImmutableList.of("binance=EOS-BTC"));
        config.setFutureNwindow(36000);
        config.setCollectNlabeled(1000);
        config.setAverageDtSBetweenLabels(0.5);
        config.setNoopThreshold(1.002f);
        config.setTruthThreshold(0.7f);
        config.setOldThresholdM(300000000);
        config.setTrainRelativeSize(0.7);
        config.setProceedFalsePositive(0.3f);
        config.setBooksPerS(1.0);
        config.setLayers(4);
        config.setLayerDim(10);
        config.setIterations(100);
        config.setLearningRate(0.006);
        config.setMomentum(0.9);
        config.setL2(0.0001);
        config.setFuturePriceGainPct(0.3);
    }

    private NnCreateTradesService tradesService() {
        commander = mock(WsGatewayCommander.class);
        tradeCreationService = mock(TradeCreationService.class);
        clientConfigCache = mock(ClientConfigCache.class);

        String name = getClientName();
        initClientConfigCache(name);
        initTradeCreationService(name);
        initGatewayOrderCreationHandler();

        return new NnCreateTradesService(commander, tradeCreationService, clientConfigCache, config);
    }

    private void initClientConfigCache(String name) {
        when(clientConfigCache.getCfg(name, TradingCurrency.EOS, TradingCurrency.Bitcoin))
                .thenReturn(Optional.of(getConfig(name)));
    }

    private ClientConfig getConfig(String name) {
        ClientConfig cfg = ClientConfig.builder()
                .client(new Client(name, true, null, null))
                .currency(TradingCurrency.EOS)
                .currencyTo(TradingCurrency.Bitcoin)
                .scaleAmount(2)
                .scalePrice(7)
                .minOrder(BigDecimal.ONE)
                .maxOrder(BigDecimal.TEN)
                .minOrderInToCurrency(new BigDecimal("0.001"))
                .tradeChargeRatePct(new BigDecimal("0.1"))
                .build();
        cfg.setEnabled(true);
        return cfg;
    }

    private void initTradeCreationService(String name) {

        when(tradeCreationService.createTradeNoSideValidation(
                any(ClientConfig.class), any(BigDecimal.class), any(BigDecimal.class), anyBoolean())
        ).thenAnswer(inv -> {
            ClientConfig cfg = inv.getArgumentAt(0, ClientConfig.class);
            BigDecimal price = inv.getArgumentAt(1, BigDecimal.class);
            BigDecimal amount = inv.getArgumentAt(2, BigDecimal.class);
            boolean isSell = inv.getArgumentAt(3, Boolean.class);

            String id = UuidGenerator.get();

            return new TradeDto(
                    null,
                    CreateOrderCommand.builder()
                            .clientName(name)
                            .currencyFrom(cfg.getCurrency().getCode())
                            .currencyTo(cfg.getCurrencyTo().getCode())
                            .price(price)
                            .amount(isSell ? amount.abs().negate() : amount.abs())
                            .id("1111")
                            .orderId(id)
                            .build()
            );
        });
    }

    private void initGatewayOrderCreationHandler() {
        doAnswer(invocation -> {
            MultiOrderCreateCommand trade = (MultiOrderCreateCommand) invocation.getArguments()[0];
            testTradeRepository.acceptTrade(trade);
            return null;
        }).when(commander).createOrders(any(MultiOrderCreateCommand.class));
    }
}
