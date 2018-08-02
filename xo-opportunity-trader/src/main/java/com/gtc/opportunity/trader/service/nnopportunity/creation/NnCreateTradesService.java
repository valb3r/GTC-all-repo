package com.gtc.opportunity.trader.service.nnopportunity.creation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.gtc.model.gateway.command.create.MultiOrderCreateCommand;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.service.UuidGenerator;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.gtc.opportunity.trader.service.dto.TradeDto;
import com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy;
import com.gtc.opportunity.trader.service.opportunity.common.TradeCreationService;
import com.gtc.opportunity.trader.service.opportunity.creation.ConfigCache;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.RejectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

import static java.math.BigDecimal.ROUND_DOWN;
import static java.math.BigDecimal.ROUND_UP;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NnCreateTradesService {

    private final Map<Strategy, Consumer<OrderBook>> handlers = ImmutableMap.of(
            Strategy.BUY_LOW_SELL_HIGH, this::handleBuySellStrategy,
            Strategy.SELL_HIGH_BUY_LOW, this::handleSellBuyStrategy
    );

    private final WsGatewayCommander commander;
    private final TradeCreationService tradeCreationService;
    private final ConfigCache configCache;

    @Transactional
    public void create(Strategy strategy, OrderBook book) {
        log.info("Creating trades for {} using {}", book, strategy);

        if (!handlers.containsKey(strategy)) {
            throw new IllegalStateException("No handler for " + strategy.name());
        }

        handlers.get(strategy).accept(book);
    }

    private void handleBuySellStrategy(OrderBook book) {
        ClientConfig config = getConfig(book);
        BigDecimal weBuyPrice = BigDecimal.valueOf(book.getBestSell());
        BigDecimal weSellPrice = weBuyPrice
                .multiply(computeGain(config))
                .setScale(config.getScalePrice(), ROUND_UP);

        BigDecimal amount = calculateAmount(config, weBuyPrice.doubleValue());

        TradeDto buy = tradeCreationService.createTradeNoSideValidation(config, weBuyPrice, amount, false);
        TradeDto sell = tradeCreationService.createTradeNoSideValidation(config, weSellPrice, amount, true);

        commander.createOrders(MultiOrderCreateCommand.builder()
                .clientName(book.getMeta().getClient())
                .id(UuidGenerator.get())
                .commands(new HashSet<>(ImmutableSet.of(buy.getCommand(), sell.getCommand())))
                .build()
        );
    }

    private void handleSellBuyStrategy(OrderBook book) {
        ClientConfig config = getConfig(book);
        BigDecimal weSellPrice = BigDecimal.valueOf(book.getBestBuy());
        BigDecimal weBuyPrice = weSellPrice
                .divide(computeGain(config), MathContext.DECIMAL128)
                .setScale(config.getScalePrice(), ROUND_DOWN);
        BigDecimal amount = calculateAmount(config, weBuyPrice.doubleValue());
        TradeDto buy = tradeCreationService.createTradeNoSideValidation(config, weBuyPrice, amount, false);
        TradeDto sell = tradeCreationService.createTradeNoSideValidation(config, weSellPrice, amount, true);

        commander.createOrders(MultiOrderCreateCommand.builder()
                .clientName(book.getMeta().getClient())
                .id(UuidGenerator.get())
                .commands(new HashSet<>(ImmutableSet.of(buy.getCommand(), sell.getCommand())))
                .build()
        );
    }

    private BigDecimal computeGain(ClientConfig cfg) {
        return cfg.getTradeChargeRatePct()
                .multiply(BigDecimal.valueOf(2)) // we do 2 trades so gain should accomodate both
                .add(BigDecimal.valueOf(cfg.getNnConfig().getFuturePriceGainPct().doubleValue()))
                .movePointLeft(2)
                .add(BigDecimal.ONE);
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
}
