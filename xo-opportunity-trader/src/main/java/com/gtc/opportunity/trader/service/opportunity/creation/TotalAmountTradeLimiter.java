package com.gtc.opportunity.trader.service.opportunity.creation;

import com.google.common.collect.ImmutableSet;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.RejectionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 27.06.18.
 */
@Service
@RequiredArgsConstructor
public class TotalAmountTradeLimiter {

    private final ClientConfigCache cache;
    private final TradeRepository tradeRepository;

    @Transactional(readOnly = true)
    public boolean canProceed(Trade trade) {
        BigDecimal bal = tradeRepository.tradeBalance(trade.getClient(), trade.getCurrencyFrom(),
                trade.getCurrencyTo(), ImmutableSet.of(TradeStatus.UNKNOWN, TradeStatus.OPENED,
                        TradeStatus.CLOSED, TradeStatus.DONE_MAN));
        ClientConfig cfg = cache.getCfg(trade.getClient().getName(), trade.getCurrencyFrom(), trade.getCurrencyTo())
                .orElseThrow(() -> new RejectionException(Reason.NO_CONFIG));

        return cfg.getSingleSideTradeLimit().compareTo(bal) <= 0;
    }
}
