package com.gtc.opportunity.trader.service.opportunity.common;

import com.gtc.meta.TradingCurrency;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import com.gtc.opportunity.trader.service.UuidGenerator;
import com.gtc.opportunity.trader.service.dto.TradeDto;
import com.gtc.opportunity.trader.service.opportunity.creation.BalanceService;
import com.gtc.opportunity.trader.service.opportunity.creation.TotalAmountTradeLimiter;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.RejectionException;
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

    private final StateMachineService<TradeStatus, TradeEvent> stateMachineService;
    private final CurrentTimestamp currentTimestamp;
    private final TradeRepository tradeRepository;
    private final Validator validator;
    private final BalanceService balanceService;
    private final TotalAmountTradeLimiter amountTradeLimiter;

    public TradeCreationService(
            @Qualifier(TRADE_MACHINE_SERVICE) StateMachineService<TradeStatus, TradeEvent> stateMachineService,
            CurrentTimestamp currentTimestamp, TradeRepository tradeRepository,
            Validator validator, BalanceService balanceService, TotalAmountTradeLimiter amountTradeLimiter) {
        this.stateMachineService = stateMachineService;
        this.currentTimestamp = currentTimestamp;
        this.tradeRepository = tradeRepository;
        this.validator = validator;
        this.balanceService = balanceService;
        this.amountTradeLimiter = amountTradeLimiter;
    }

    @Transactional
    public TradeDto createTradeNoSideValidation(ClientConfig cfg, BigDecimal price, BigDecimal amount, boolean isSell) {
        String id = UuidGenerator.get();

        CreateOrderCommand comm = CreateOrderCommand.builder()
                .clientName(cfg.getClient().getName())
                .currencyFrom(cfg.getCurrency().getCode())
                .currencyTo(cfg.getCurrencyTo().getCode())
                .price(price)
                .amount(isSell ? amount.abs().negate() : amount.abs())
                .id(id)
                .orderId(id)
                .build();

        if (!validator.validate(comm).isEmpty() || 0 == comm.getAmount().compareTo(BigDecimal.ZERO)) {
            log.error("Validation issue {}", comm);
            throw new RejectionException(Reason.VALIDATION_FAIL);
        }

        return persistAndProceed(comm, cfg, false);
    }

    @Transactional
    public TradeDto createTrade(ClientConfig cfg, BigDecimal price, BigDecimal amount, boolean isSell) {
        String id = UuidGenerator.get();

        CreateOrderCommand comm = CreateOrderCommand.builder()
                .clientName(cfg.getClient().getName())
                .currencyFrom(cfg.getCurrency().getCode())
                .currencyTo(cfg.getCurrencyTo().getCode())
                .price(price)
                .amount(isSell ? amount.abs().negate() : amount.abs())
                .id(id)
                .orderId(id)
                .build();

        if (!validator.validate(comm).isEmpty() || 0 == comm.getAmount().compareTo(BigDecimal.ZERO)) {
            log.error("Validation issue {}", comm);
            throw new RejectionException(Reason.VALIDATION_FAIL);
        }

        return persistAndProceed(comm, cfg, true);
    }

    private TradeDto persistAndProceed(CreateOrderCommand comm, ClientConfig cfg,  boolean validateSingleSide) {
        Trade trade = buildTrade(comm, cfg);

        if (!balanceService.canProceed(trade)) {
            throw new RejectionException(Reason.LOW_BAL);
        }

        // side limiting rejections can apply only to cross-market trades
        if (validateSingleSide && !amountTradeLimiter.canProceed(trade)) {
            throw new RejectionException(Reason.SIDE_LIMIT);
        }

        balanceService.proceed(trade);
        tradeRepository.save(trade);
        StateMachine<TradeStatus, TradeEvent> machine = stateMachineService.acquireStateMachine(trade.getId());
        stateMachineService.releaseStateMachine(machine.getId());

        return new TradeDto(trade, comm);
    }

    private Trade buildTrade(CreateOrderCommand comm, ClientConfig config) {
        return Trade.builder()
                .id(comm.getId())
                .assignedId(comm.getId())
                .client(config.getClient())
                .openingPrice(comm.getPrice())
                .openingAmount(comm.getAmount())
                .price(comm.getPrice())
                .amount(comm.getAmount())
                .expectedReverseAmount(expectedReverseAmount(comm, config))
                .currencyFrom(TradingCurrency.fromCode(comm.getCurrencyFrom()))
                .currencyTo(TradingCurrency.fromCode(comm.getCurrencyTo()))
                .isSell(isSell(comm))
                .lastMessageId(comm.getId())
                .status(TradeStatus.UNKNOWN)
                .statusUpdated(currentTimestamp.dbNow())
                .build();
    }

    private BigDecimal expectedReverseAmount(CreateOrderCommand comm, ClientConfig cfg) {
        BigDecimal charge = BigDecimal.ONE.subtract(cfg.getTradeChargeRatePct().movePointLeft(2));
        if (isSell(comm)) {
            return comm.getAmount().multiply(comm.getPrice()).multiply(charge).abs();
        }

        return comm.getAmount().multiply(charge).abs().negate();
    }

    private boolean isSell(CreateOrderCommand comm) {
        return comm.getAmount().compareTo(BigDecimal.ZERO) < 0;
    }
}
