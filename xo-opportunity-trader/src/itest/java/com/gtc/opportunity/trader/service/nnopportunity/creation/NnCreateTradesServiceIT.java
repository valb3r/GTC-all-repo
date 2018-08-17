package com.gtc.opportunity.trader.service.nnopportunity.creation;

import com.google.common.collect.Lists;
import com.gtc.meta.CurrencyPair;
import com.gtc.model.provider.ByClientAndCurrency;
import com.gtc.model.provider.OrderBook;
import com.gtc.opportunity.trader.BaseInitializedIT;
import com.gtc.opportunity.trader.domain.AcceptedNnTrade;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.repository.AcceptedNnTradeRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy;
import com.gtc.opportunity.trader.service.nnopportunity.repository.StrategyDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Created by Valentyn Berezin on 02.08.18.
 */
public class NnCreateTradesServiceIT extends BaseInitializedIT {

    private static final double CONFIDENCE = 0.8;
    private static final int MODEL_AGE_S = 15000;
    private static final int AVG_NOOP_LABEL_AGE_S = 12345;
    private static final int AVG_ACT_LABEL_AGE_S = 12346;

    private static final double BEST_SELL = 0.0011;
    private static final double BEST_BUY = 0.001;

    @Autowired
    private NnCreateTradesService tradesService;

    @Autowired
    private AcceptedNnTradeRepository nnTradeRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Test
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

        List<AcceptedNnTrade> nnTrades = Lists.newArrayList(nnTradeRepository.findAll());
        assertThat(nnTrades.size()).isEqualTo(1);

        AcceptedNnTrade trade = nnTrades.get(0);

        List<Trade> trades = Lists.newArrayList(tradeRepository.findAll());
        assertThat(trades.size()).isEqualTo(2);
        assertThat(trades.get(0).getNnOrder()).isEqualTo(trade);
        assertThat(trades.get(1).getNnOrder()).isEqualTo(trade);
    }
}
