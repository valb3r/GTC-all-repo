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
import com.gtc.opportunity.trader.service.TradeCreationService;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.gtc.opportunity.trader.service.dto.TradeDto;
import com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.FeeFitted;
import com.gtc.opportunity.trader.service.nnopportunity.creation.fitter.FeeFitter;
import com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy;
import com.gtc.opportunity.trader.service.nnopportunity.repository.StrategyDetails;
import com.gtc.opportunity.trader.service.xoopportunity.creation.ConfigCache;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.RejectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.BiConsumer;

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
    private final FeeFitter fitter;

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
        FeeFitted fitted = fitter.buyLowSellHigh(book, config);

        TradeDto buy = tradeCreationService.createTradeNoSideValidation(
                null, config, fitted.getBuyPrice(), fitted.getBuyAmount(), false, true);
        TradeDto sell = tradeCreationService.createTradeNoSideValidation(
                buy.getTrade(), config, fitted.getSellPrice(), fitted.getSellAmount(), true, false);

        buy.getCommand().setRetryStrategy(RetryStrategy.BASIC_RETRY);

        persistNnTrade(fitted, config, buy, sell, details);

        commander.createOrder(buy.getCommand());
    }

    private void handleSellBuyStrategy(StrategyDetails details, OrderBook book) {
        ClientConfig config = getConfig(book);

        FeeFitted fitted = fitter.sellHighBuyLow(book, config);

        TradeDto sell = tradeCreationService.createTradeNoSideValidation(
                null, config, fitted.getSellPrice(), fitted.getSellAmount(), true, true);
        TradeDto buy = tradeCreationService.createTradeNoSideValidation(
                sell.getTrade(), config, fitted.getBuyPrice(), fitted.getBuyAmount(), false, false);

        sell.getCommand().setRetryStrategy(RetryStrategy.BASIC_RETRY);

        persistNnTrade(fitted, config, buy, sell, details);

        commander.createOrder(sell.getCommand());
    }

    private ClientConfig getConfig(OrderBook book) {
        return configCache.getClientCfg(
                book.getMeta().getClient(),
                book.getMeta().getPair().getFrom(),
                book.getMeta().getPair().getTo()
        ).orElseThrow(() -> new RejectionException(Reason.NO_CONFIG));
    }

    private void persistNnTrade(FeeFitted fitted, ClientConfig config, TradeDto first, TradeDto second,
                                StrategyDetails details) {
        AcceptedNnTrade nnTrade = AcceptedNnTrade.builder()
                .client(config.getClient())
                .currencyFrom(config.getCurrency())
                .currencyTo(config.getCurrencyTo())
                .amount(fitted.getAmount())
                .priceFromBuy(first.getTrade().getOpeningPrice())
                .priceToSell(second.getTrade().getOpeningPrice())
                .expectedDeltaFrom(fitted.getProfitFrom())
                .expectedDeltaTo(fitted.getProfitTo())
                .confidence(details.getConfidence())
                .strategy(details.getStrategy())
                .modelAgeS(details.getModelAgeS())
                .averageNoopLabelAgeS(details.getAvgNoopLabelAgeS())
                .averageActLabelAgeS(details.getAvgActLabelAgeS())
                .status(NnAcceptStatus.MASTER_UNKNOWN)
                .build();
        nnTrade = nnTradeRepository.save(nnTrade);
        Trade firstTrade = first.getTrade();
        Trade secondTrade = second.getTrade();

        firstTrade.setNnOrder(nnTrade);
        secondTrade.setNnOrder(nnTrade);

        saveDependant(firstTrade);
        saveDependant(secondTrade);

        tradeRepository.save(first.getTrade());
        tradeRepository.save(second.getTrade());
    }

    private void saveDependant(Trade trade) {
        if (null != trade.getDependsOn()) {
            trade.setDependsOn(tradeRepository.save(trade.getDependsOn()));
        }
    }
}
