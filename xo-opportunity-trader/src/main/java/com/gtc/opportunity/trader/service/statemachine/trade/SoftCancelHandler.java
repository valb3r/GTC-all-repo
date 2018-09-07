package com.gtc.opportunity.trader.service.statemachine.trade;

import com.google.common.collect.ImmutableSet;
import com.gtc.opportunity.trader.domain.SoftCancel;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.SoftCancelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static com.gtc.opportunity.trader.domain.TradeStatus.*;

/**
 * Created by Valentyn Berezin on 04.09.18.
 */
@Service
@RequiredArgsConstructor
public class SoftCancelHandler {

    private static final Set<TradeStatus> CANCELLATION = ImmutableSet.of(
            ERR_OPEN, NEED_RETRY, GEN_ERR, CANCELLED);

    private final SoftCancelRepository softCancelRepository;

    @Transactional
    public void updateSoftCancelIfNeeded(Trade trade, TradeStatus status) {
        if (null == trade.getDependsOn()) {
            return;
        }

        softCancelRepository
                .findForTrade(trade.getClient().getName(), trade.getCurrencyFrom(), trade.getCurrencyTo())
                .ifPresent(softCancel ->
                        softCancelRepository.save(updateSoftCancel(softCancel, status))
                );
    }

    private SoftCancel updateSoftCancel(SoftCancel cancel, TradeStatus status) {
        if (TradeStatus.CLOSED == status) {
            cancel.setDone(cancel.getDone() + 1);
        } else if (CANCELLATION.contains(status)) {
            cancel.setCancelled(cancel.getCancelled() + 1);
        }

        return cancel;
    }

}
