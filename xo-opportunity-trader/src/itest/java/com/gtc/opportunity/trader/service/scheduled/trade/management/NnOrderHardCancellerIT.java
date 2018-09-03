package com.gtc.opportunity.trader.service.scheduled.trade.management;

import com.gtc.model.gateway.command.manage.CancelOrderCommand;
import com.gtc.opportunity.trader.BaseNnTradeInitialized;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by Valentyn Berezin on 21.08.18.
 */
public class NnOrderHardCancellerIT extends BaseNnTradeInitialized {

    @Autowired
    private CurrentTimestamp dbTime;

    @Autowired
    private NnOrderHardCanceller canceller;

    @MockBean
    private WsGatewayCommander commander;

    @Test
    public void testCancelsSlaveKeepsMaster() {
        expireSlave(TradeStatus.OPENED);

        canceller.hardCancel();

        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.UNKNOWN);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.CANCELLED);
        verify(commander).cancel(any(CancelOrderCommand.class));
    }

    @Test
    public void testCancelsMasterKeepsSlave() {
        bindSlaveToOther();
        expireMaster(TradeStatus.OPENED);

        canceller.hardCancel();

        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.CANCELLED);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.DEPENDS_ON);
        verify(commander).cancel(any(CancelOrderCommand.class));
    }

    @Test
    public void testCancelsAll() {
        bindSlaveToOther();
        expireSlave(TradeStatus.OPENED);
        expireMaster(TradeStatus.OPENED);

        canceller.hardCancel();

        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.CANCELLED);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.CANCELLED);
        verify(commander, times(2)).cancel(any(CancelOrderCommand.class));
    }

    @Test
    public void testCancelsNone() {
        canceller.hardCancel();

        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.UNKNOWN);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.DEPENDS_ON);
        verify(commander, never()).cancel(any(CancelOrderCommand.class));
    }

    @Test
    public void testCancelsNoneIfNotOpen() {
        expireSlave(TradeStatus.UNKNOWN);
        expireMaster(TradeStatus.UNKNOWN);

        canceller.hardCancel();

        assertThat(tradeRepository.findById(TRADE_ONE)).map(Trade::getStatus).contains(TradeStatus.UNKNOWN);
        assertThat(tradeRepository.findById(TRADE_TWO)).map(Trade::getStatus).contains(TradeStatus.UNKNOWN);
    }

    private void expireSlave(TradeStatus status) {
        createdSlaveTradeBuy.setStatusUpdated(dbTime.dbNow().minusHours(EXPIRE_OPEN_H + 1L));
        createdSlaveTradeBuy.setStatus(status);
        tradeRepository.save(createdSlaveTradeBuy);
    }

    private void expireMaster(TradeStatus status) {
        createdMasterTradeSell.setRecordedOn(dbTime.dbNow().minusMinutes(MAX_SLAVE_DELAY_M + 1L));
        createdMasterTradeSell.setStatus(status);
        tradeRepository.save(createdMasterTradeSell);
    }

    private void bindSlaveToOther() {
        Trade other = tradeRepository.save(Trade.builder()
                .id("")
                .assignedId("")
                .client(createdClient)
                .currencyFrom(FROM)
                .currencyTo(TO)
                .nnOrder(createdNnTrade)
                .dependsOn(null)
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
        createdSlaveTradeBuy.setDependsOn(other);
        createdSlaveTradeBuy = tradeRepository.save(createdSlaveTradeBuy);
    }
}
