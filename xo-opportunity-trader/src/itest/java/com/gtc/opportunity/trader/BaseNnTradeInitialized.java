package com.gtc.opportunity.trader;

import com.gtc.opportunity.trader.domain.AcceptedNnTrade;
import com.gtc.opportunity.trader.domain.NnAcceptStatus;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.AcceptedNnTradeRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.nnopportunity.repository.Strategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by Valentyn Berezin on 03.08.18.
 */
@Transactional
public abstract class BaseNnTradeInitialized extends BaseInitializedIT {

    protected static final String TRADE_ONE = "one";
    protected static final String TRADE_TWO = "two";

    @Autowired
    protected AcceptedNnTradeRepository acceptedRepository;

    @Autowired
    protected TradeRepository tradeRepository;

    protected AcceptedNnTrade createdNnTrade;
    protected Trade createdTradeSell;
    protected Trade createdTradeBuy;

    @BeforeTransaction
    public void buildTrades() {
        createdNnTrade = acceptedRepository.save(AcceptedNnTrade.builder()
                .client(createdClient)
                .currencyFrom(FROM)
                .currencyTo(TO)
                .amount(BigDecimal.TEN)
                .priceFromBuy(BigDecimal.TEN)
                .priceToSell(BigDecimal.TEN)
                .expectedDeltaFrom(BigDecimal.TEN)
                .expectedDeltaTo(BigDecimal.TEN)
                .confidence(0.5)
                .strategy(Strategy.BUY_LOW_SELL_HIGH)
                .status(NnAcceptStatus.UNCONFIRMED)
                .build());

        createdTradeSell = tradeRepository.save(Trade.builder()
                .id(TRADE_ONE)
                .assignedId(TRADE_ONE)
                .client(createdClient)
                .currencyFrom(FROM)
                .currencyTo(TO)
                .nnOrder(createdNnTrade)
                .openingAmount(BigDecimal.ONE)
                .openingAmount(BigDecimal.TEN)
                .amount(BigDecimal.ONE)
                .price(BigDecimal.TEN)
                .isSell(false)
                .statusUpdated(LocalDateTime.now())
                .expectedReverseAmount(BigDecimal.ONE)
                .status(TradeStatus.UNKNOWN)
                .wallet(walletFrom)
                .build());

        createdTradeBuy = tradeRepository.save(Trade.builder()
                .id(TRADE_TWO)
                .assignedId(TRADE_TWO)
                .client(createdClient)
                .currencyFrom(FROM)
                .currencyTo(TO)
                .nnOrder(createdNnTrade)
                .openingAmount(BigDecimal.ONE)
                .openingAmount(BigDecimal.TEN)
                .amount(BigDecimal.ONE)
                .price(BigDecimal.TEN)
                .isSell(true)
                .statusUpdated(LocalDateTime.now())
                .expectedReverseAmount(BigDecimal.ONE)
                .status(TradeStatus.UNKNOWN)
                .wallet(walletTo)
                .build());
    }
}
