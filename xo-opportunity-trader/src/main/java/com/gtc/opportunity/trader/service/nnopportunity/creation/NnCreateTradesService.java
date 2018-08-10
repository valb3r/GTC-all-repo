package com.gtc.opportunity.trader.service.nnopportunity.creation;

import com.google.common.collect.ImmutableMap;
import com.gtc.model.gateway.RetryStrategy;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.domain.AcceptedNnTrade;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.NnAcceptStatus;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.repository.AcceptedNnTradeRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.gtc.opportunity.trader.service.dto.TradeDto;
import com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy;
import com.gtc.opportunity.trader.service.nnopportunity.repository.StrategyDetails;
import com.gtc.opportunity.trader.service.xoopportunity.common.TradeCreationService;
import com.gtc.opportunity.trader.service.xoopportunity.creation.ConfigCache;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.RejectionException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.BiConsumer;

import static java.math.BigDecimal.ROUND_DOWN;
import static java.math.BigDecimal.ROUND_UP;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NnCreateTradesService {

    private final Map<Strategy, BiConsumer<StrategyDetails, OrderBook>> handlers = ImmutableMap.of(
            Strategy.BUY_LOW_SELL_HIGH, this::handleBuySellStrategy,
            Strategy.SELL_HIGH_BUY_LOW, this::handleSellBuyStrategy
    );

    private final WsGatewayCommander commander;
    private final TradeCreationService tradeCreationService;
    private final ConfigCache configCache;
    private final AcceptedNnTradeRepository nnTradeRepository;
    private final TradeRepository tradeRepository;

    @Transactional
    public void create(StrategyDetails strategy, OrderBook book) {
        log.info("Creating trades for {} using {}", book, strategy);

        if (!handlers.containsKey(strategy.getStrategy())) {
            throw new IllegalStateException("No handler for " + strategy.getStrategy().name());
        }

        handlers.get(strategy.getStrategy()).accept(strategy, book);
    }

    private void handleBuySellStrategy(StrategyDetails details, OrderBook book) {
        ClientConfig config = getConfig(book);
        BigDecimal weBuyPrice = BigDecimal.valueOf(book.getBestSell());
        BigDecimal weSellPrice = weBuyPrice
                .multiply(computeGain(config))
                .setScale(config.getScalePrice(), ROUND_UP);

        BigDecimal amount = calculateAmount(config, weBuyPrice.doubleValue());

        TradeDto buy = tradeCreationService
                .createTradeNoSideValidation(null, config, weBuyPrice, amount, false, true);
        TradeDto sell = tradeCreationService
                .createTradeNoSideValidation(buy.getTrade(), config, weSellPrice, amount, true, false);

        buy.getCommand().setRetryStrategy(RetryStrategy.BASIC_RETRY);

        persistNnTrade(config, buy, sell, details);

        commander.createOrder(buy.getCommand());
    }

    private void handleSellBuyStrategy(StrategyDetails details, OrderBook book) {
        ClientConfig config = getConfig(book);
        BigDecimal weSellPrice = BigDecimal.valueOf(book.getBestBuy());
        BigDecimal weBuyPrice = weSellPrice
                .divide(computeGain(config), MathContext.DECIMAL128)
                .setScale(config.getScalePrice(), ROUND_DOWN);
        BigDecimal amount = calculateAmount(config, weBuyPrice.doubleValue());

        TradeDto sell = tradeCreationService
                .createTradeNoSideValidation(null, config, weSellPrice, amount, true, true);
        TradeDto buy = tradeCreationService
                .createTradeNoSideValidation(sell.getTrade(), config, weBuyPrice, amount, false, false);

        sell.getCommand().setRetryStrategy(RetryStrategy.BASIC_RETRY);

        persistNnTrade(config, sell, buy, details);

        commander.createOrder(sell.getCommand());
    }

    private BigDecimal computeGain(ClientConfig cfg) {
        BigDecimal charge = computeCharge(cfg);
        return BigDecimal.ONE.add(cfg.getNnConfig().getFuturePriceGainPct().movePointLeft(2))
                .divide(charge.multiply(charge), MathContext.DECIMAL128);
    }

    private BigDecimal calculateAmount(ClientConfig config, double minPrice) {
        return BigDecimal.valueOf(calculateMinAmountWithNotional(config, minPrice))
                .multiply(BigDecimal.valueOf(2))
                .setScale(config.getScaleAmount(), RoundingMode.CEILING);
    }
    private ClientConfig getConfig(OrderBook book) {
        return configCache.getClientCfg(
                book.getMeta().getClient(),
                book.getMeta().getPair().getFrom(),
                book.getMeta().getPair().getTo()
        ).orElseThrow(() -> new RejectionException(Reason.NO_CONFIG));
    }

    private double calculateMinAmountWithNotional(ClientConfig cfg, double price) {
        if (null == cfg.getMinOrderInToCurrency()) {
            return cfg.getMinOrder().doubleValue();
        }

        return Math.max(cfg.getMinOrderInToCurrency().doubleValue() / price, cfg.getMinOrder().doubleValue());
    }

    private void persistNnTrade(ClientConfig config, TradeDto first, TradeDto second, StrategyDetails details) {
        BigDecimal amount = amount(first, second);
        Profits profits = profitInFromCurrency(config, first, second);
        AcceptedNnTrade trade = AcceptedNnTrade.builder()
                .client(config.getClient())
                .currencyFrom(config.getCurrency())
                .currencyTo(config.getCurrencyTo())
                .amount(amount)
                .priceFromBuy(first.getTrade().getOpeningPrice())
                .priceToSell(second.getTrade().getOpeningPrice())
                .expectedDeltaFrom(profits.getFrom())
                .expectedDeltaTo(profits.getTo())
                .confidence(details.getConfidence())
                .strategy(details.getStrategy())
                .modelAgeS(details.getModelAgeS())
                .averageNoopLabelAgeS(details.getAvgNoopLabelAgeS())
                .averageActLabelAgeS(details.getAvgActLabelAgeS())
                .status(NnAcceptStatus.MASTER_UNKNOWN)
                .build();
        trade = nnTradeRepository.save(trade);

        // FIXME: this should be dealt via cascading
        first.getTrade().setNnOrder(trade);
        second.getTrade().setNnOrder(trade);
        tradeRepository.save(first.getTrade());
        tradeRepository.save(second.getTrade());
    }

    private static BigDecimal amount(TradeDto buy, TradeDto sell) {
        return buy.getTrade().getOpeningAmount().abs().max(sell.getTrade().getOpeningAmount().abs());
    }

    private static Profits profitInFromCurrency(ClientConfig config, TradeDto buyTrade, TradeDto sellTrade) {
        Trade buy = buyTrade.getTrade();
        Trade sell = sellTrade.getTrade();
        BigDecimal charge = computeCharge(config);

        return new Profits(
            buy.getOpeningAmount().multiply(charge).add(sell.getOpeningAmount()),
            sell.getOpeningAmount().abs().multiply(sell.getOpeningPrice()).multiply(charge)
                    .subtract(buy.getOpeningAmount().multiply(buy.getOpeningPrice()))
        );
    }

    private static BigDecimal computeCharge(ClientConfig config) {
        return BigDecimal.ONE.subtract(config.getTradeChargeRatePct().movePointLeft(2));
    }

    @Data
    private static class Profits {

        private final BigDecimal from;
        private final BigDecimal to;
    }
}
