package com.gtc.opportunity.trader;

import com.gtc.opportunity.trader.domain.AcceptedNnTrade;
import com.gtc.opportunity.trader.domain.NnAcceptStatus;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.AcceptedNnTradeRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
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

    @Autowired
    protected CurrentTimestamp dbTime;

    protected AcceptedNnTrade createdNnTrade;
    protected Trade createdMasterTradeSell;
    protected Trade createdSlaveTradeBuy;

    @BeforeTransaction
    public void buildTrades() {
        createdNnTrade = acceptedRepository.save(AcceptedNnTrade.builder()
                .client(createdClient)
                .currencyFrom(FROM)
                .currencyTo(TO)
                .amount(BigDecimal.ONE)
                .priceFromBuy(BigDecimal.TEN)
                .priceToSell(BigDecimal.TEN)
                .expectedDeltaFrom(BigDecimal.TEN)
                .expectedDeltaTo(BigDecimal.TEN)
                .confidence(0.5)
                .strategy(Strategy.BUY_LOW_SELL_HIGH)
                .status(NnAcceptStatus.MASTER_UNKNOWN)
                .build());

        createdMasterTradeSell = tradeRepository.save(Trade.builder()
                .id(TRADE_ONE)
                .assignedId(TRADE_ONE)
                .client(createdClient)
                .currencyFrom(FROM)
                .currencyTo(TO)
                .nnOrder(createdNnTrade)
                .openingAmount(BigDecimal.ONE)
                .openingPrice(BigDecimal.ONE)
                .amount(BigDecimal.ONE)
                .price(BigDecimal.ONE)
                .isSell(false)
                .statusUpdated(LocalDateTime.now())
                .expectedReverseAmount(BigDecimal.ONE)
                .status(TradeStatus.UNKNOWN)
                .wallet(walletFrom)
                .build());

        createdSlaveTradeBuy = tradeRepository.save(Trade.builder()
                .id(TRADE_TWO)
                .assignedId(TRADE_TWO)
                .client(createdClient)
                .currencyFrom(FROM)
                .currencyTo(TO)
                .nnOrder(createdNnTrade)
                .dependsOn(createdMasterTradeSell)
                .openingAmount(BigDecimal.ONE)
                .openingPrice(BigDecimal.ONE)
                .amount(BigDecimal.ONE)
                .price(BigDecimal.ONE)
                .isSell(true)
                .statusUpdated(LocalDateTime.now())
                .expectedReverseAmount(BigDecimal.ONE)
                .status(TradeStatus.DEPENDS_ON)
                .wallet(walletTo)
                .build());
    }

    protected void expireSlave(TradeStatus status) {
        createdSlaveTradeBuy = tradeRepository.findById(TRADE_TWO).get();
        createdSlaveTradeBuy.setStatusUpdated(dbTime.dbNow().minusHours(EXPIRE_OPEN_H + 1L));
        createdSlaveTradeBuy.setStatus(status);
        tradeRepository.save(createdSlaveTradeBuy);
    }

    protected void expireMaster(TradeStatus status) {
        createdMasterTradeSell = tradeRepository.findById(TRADE_ONE).get();
        createdMasterTradeSell.setRecordedOn(dbTime.dbNow().minusMinutes(MAX_SLAVE_DELAY_M + 1L));
        createdMasterTradeSell.setStatus(status);
        tradeRepository.save(createdMasterTradeSell);
    }
}
