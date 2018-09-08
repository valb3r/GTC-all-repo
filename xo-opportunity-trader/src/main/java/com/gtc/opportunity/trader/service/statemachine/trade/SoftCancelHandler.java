package com.gtc.opportunity.trader.service.statemachine.trade;

import com.google.common.collect.ImmutableSet;
import com.gtc.opportunity.trader.domain.SoftCancel;
import com.gtc.opportunity.trader.domain.SoftCancelConfig;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.SoftCancelConfigRepository;
import com.gtc.opportunity.trader.repository.SoftCancelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
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

    private final SoftCancelConfigRepository cancelConfigRepository;
    private final SoftCancelRepository softCancelRepository;

    @Transactional
    public void updateSoftCancelIfNeeded(Trade trade, TradeStatus status) {
        if (null == trade.getDependsOn()) {
            return;
        }

        Optional<SoftCancelConfig> config = cancelConfigRepository.findForTrade(trade);

        if(!config.isPresent()) {
            return;
        }

        SoftCancel cancel = softCancelRepository
                .findForTrade(trade)
                .orElseGet(() -> softCancelRepository.save(SoftCancel.builder()
                        .done(0)
                        .cancelled(0)
                        .cancelConfig(config.get())
                        .build())
                );

        softCancelRepository.save(updateSoftCancel(cancel, status));
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
