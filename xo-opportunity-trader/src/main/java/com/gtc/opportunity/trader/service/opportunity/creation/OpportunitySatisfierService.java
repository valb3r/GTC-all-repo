package com.gtc.opportunity.trader.service.opportunity.creation;

import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.service.dto.PreciseXoAmountDto;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.RejectionException;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.XoTransactionCalculator;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.XoTradeCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Valentyn Berezin on 24.02.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunitySatisfierService {

    private final CreateTradesService creationService;
    private final RateCheckingService rateCheckingService;
    private final OpportunitySatisifactionLimiter satisifactionLimiter;
    private final XoTransactionCalculator calculator;

    // since only 2 blocking events - balance and rate limiting can prevent transaction to happen
    // and both are insert-based, we increase isolation and acquire new trx.
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public void satisfyOpportunity(ClientConfig from, ClientConfig to, FullCrossMarketOpportunity opportunity) {
        if (!rateCheckingService.ratePass(from, to)) {
            throw new RejectionException(Reason.RATE_TOO_HIGH);
        }

        XoTradeCondition transactCond = satisifactionLimiter.calculateAmount(from, to, opportunity);
        PreciseXoAmountDto preciseAmount = calculator.calculate(transactCond);
        creationService.createTrades(opportunity, preciseAmount, from, to);
    }
}
