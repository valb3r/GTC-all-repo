package com.gtc.opportunity.trader.service.statemachine.trade;

import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Created by Valentyn Berezin on 11.08.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceReleaser {

    private final StateMachineService<TradeStatus, TradeEvent> stateMachineService;
    private final WalletRepository walletRepository;
    private final TradeRepository tradeRepository;

    @Transactional
    public void release(String tradeId) {
        release(tradeId, false);
    }

    @Transactional
    public void release(String tradeId, boolean doCancelDependant) {
        tradeRepository.findById(tradeId).filter(it -> !it.isBalanceReleased()).ifPresent(trade ->
                release(trade, doCancelDependant)
        );
    }

    private void release(Trade trade, boolean doCancelDependant) {
        BigDecimal toRelease = trade.amountReservedOnWallet(trade.getWallet())
                .orElseThrow(() -> new IllegalStateException("Can't release amount on wallet"));
        log.info("Releasing {} on wallet id {} from trade {}",
                toRelease,
                trade.getWallet().getId(),
                trade.getId());
        BigDecimal reserved = trade.getWallet().getReservedBalance();
        trade.getWallet().setReservedBalance(reserved.subtract(toRelease));
        trade.setBalanceReleased(true);
        tradeRepository.save(trade);
        walletRepository.save(trade.getWallet());

        if (!doCancelDependant) {
            return;
        }

        tradeRepository.findByDependsOn(trade).ifPresent(dependnant -> {
            stateMachineService.acquireStateMachine(dependnant.getId()).sendEvent(TradeEvent.CANCELLED);
            stateMachineService.releaseStateMachine(dependnant.getId());
        });
    }
}
