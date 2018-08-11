package com.gtc.opportunity.trader.service;

import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.repository.WalletRepository;
import com.gtc.opportunity.trader.service.dto.TradeDto;
import com.gtc.opportunity.trader.service.xoopportunity.creation.BalanceService;
import com.gtc.opportunity.trader.service.xoopportunity.creation.TotalAmountTradeLimiter;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.RejectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.math.BigDecimal;

import static com.gtc.opportunity.trader.config.statemachine.TradeStateMachineConfig.TRADE_MACHINE_SERVICE;

/**
 * Created by Valentyn Berezin on 28.02.18.
 */
@Slf4j
@Service
public class TradeCreationService {

    private final WalletRepository walletRepository;
    private final StateMachineService<TradeStatus, TradeEvent> stateMachineService;
    private final CurrentTimestamp currentTimestamp;
    private final TradeRepository tradeRepository;
    private final Validator validator;
    private final BalanceService balanceService;
    private final TotalAmountTradeLimiter amountTradeLimiter;

    public TradeCreationService(
            @Qualifier(TRADE_MACHINE_SERVICE) StateMachineService<TradeStatus, TradeEvent> stateMachineService,
            CurrentTimestamp currentTimestamp, TradeRepository tradeRepository,
            Validator validator, BalanceService balanceService, TotalAmountTradeLimiter amountTradeLimiter,
            WalletRepository walletRepository) {
        this.stateMachineService = stateMachineService;
        this.currentTimestamp = currentTimestamp;
        this.tradeRepository = tradeRepository;
        this.balanceService = balanceService;
        this.amountTradeLimiter = amountTradeLimiter;
        this.validator = validator;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public TradeDto createTradeNoSideValidation(Trade dependsOn, ClientConfig cfg, BigDecimal price, BigDecimal amount,
                                                boolean isSell, boolean validateBalance) {
        return persistAndProceed(dependsOn, cfg, price, amount, isSell, false, validateBalance);
    }

    @Transactional
    public TradeDto createTrade(Trade dependsOn, ClientConfig cfg, BigDecimal price, BigDecimal amount, boolean isSell,
                                boolean validateBalance) {
        return persistAndProceed(dependsOn, cfg, price, amount, isSell, true, validateBalance);
    }

    public CreateOrderCommand map(Trade trade) {
        CreateOrderCommand comm = CreateOrderCommand.builder()
                .clientName(trade.getClient().getName())
                .currencyFrom(trade.getCurrencyFrom().getCode())
                .currencyTo(trade.getCurrencyTo().getCode())
                .price(trade.getOpeningPrice().stripTrailingZeros()) // db does not preserve precision
                .amount(trade.getOpeningAmount().stripTrailingZeros()) // db does not preserve precision
                .id(trade.getId())
                .orderId(trade.getId())
                .build();

        if (!validator.validate(comm).isEmpty() || 0 == comm.getAmount().compareTo(BigDecimal.ZERO)) {
            log.error("Validation issue {}", comm);
            throw new RejectionException(Reason.VALIDATION_FAIL);
        }

        return comm;
    }

    private TradeDto persistAndProceed(Trade dependsOn, ClientConfig cfg, BigDecimal price, BigDecimal amount,
                                       boolean isSell, boolean validateSingleSide, boolean validateBalance) {
        Trade trade = buildTrade(dependsOn, cfg, price, amount, isSell);
        trade.setDependsOn(dependsOn);

        if (validateBalance && !balanceService.canProceed(trade)) {
            throw new RejectionException(Reason.LOW_BAL);
        }

        // side limiting rejections can apply only to cross-market trades
        if (validateSingleSide && !amountTradeLimiter.canProceed(trade)) {
            throw new RejectionException(Reason.SIDE_LIMIT);
        }

        balanceService.proceed(trade);
        trade = tradeRepository.save(trade);
        reserveBalance(trade);

        StateMachine<TradeStatus, TradeEvent> machine = stateMachineService.acquireStateMachine(trade.getId());

        if (null == dependsOn) {
            machine.sendEvent(TradeEvent.DEPENDENCY_DONE);
        }

        stateMachineService.releaseStateMachine(machine.getId());

        return new TradeDto(trade, map(trade));
    }

    private void reserveBalance(Trade trade) {
        BigDecimal reserve = trade.amountReservedOnWallet(trade.getWallet())
                .orElseThrow(() -> new IllegalStateException("Wrong trade state - no wallet"));
        trade.getWallet().setReservedBalance(trade.getWallet().getReservedBalance().add(reserve));
        walletRepository.save(trade.getWallet());
    }

    private Trade buildTrade(Trade dependsOn, ClientConfig cfg, BigDecimal price, BigDecimal amount, boolean isSell) {
        String id = UuidGenerator.get();

        BigDecimal commandAmount = isSell ? amount.abs().negate() : amount.abs();
        return Trade.builder()
                .id(id)
                .assignedId(id)
                .client(cfg.getClient())
                .openingPrice(price)
                .openingAmount(commandAmount)
                .price(price)
                .amount(commandAmount)
                .expectedReverseAmount(expectedReverseAmount(price, amount, cfg, isSell))
                .currencyFrom(cfg.getCurrency())
                .currencyTo(cfg.getCurrencyTo())
                .isSell(isSell)
                .lastMessageId(id)
                .status(null == dependsOn ? TradeStatus.UNKNOWN : TradeStatus.DEPENDS_ON)
                .statusUpdated(currentTimestamp.dbNow())
                .dependsOn(dependsOn)
                .build();
    }

    private BigDecimal expectedReverseAmount(BigDecimal price, BigDecimal amount, ClientConfig cfg, boolean isSell) {
        BigDecimal charge = BigDecimal.ONE.subtract(cfg.getTradeChargeRatePct().movePointLeft(2));
        if (isSell) {
            return amount.multiply(price).multiply(charge).abs();
        }

        return amount.multiply(charge).abs().negate();
    }
}
