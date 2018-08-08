package com.gtc.opportunity.trader.service.statemachine.nnaccept;

import com.gtc.model.gateway.RetryStrategy;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.gtc.opportunity.trader.service.xoopportunity.common.TradeCreationService;
import com.gtc.opportunity.trader.service.xoopportunity.creation.BalanceService;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.RejectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Valentyn Berezin on 08.08.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NnDependencyHandler {

    private final BalanceService balanceService;
    private final TradeCreationService creationService;
    private final WsGatewayCommander commander;
    private final NnDependencyPropagator dependencyPropagator;

    @Retryable(value = RejectionException.class,
            maxAttemptsExpression = "5",
            backoff = @Backoff(delay = 10000L, multiplier = 3)
    )
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void dependencyDone(Trade dependent) {
        if (!balanceService.canProceed(dependent)) {
            log.warn("Can't proceed with dependent due to low balace");
            throw new RejectionException(Reason.LOW_BAL);
        }

        dependencyPropagator.ackDependencyDone(dependent.getId());
        CreateOrderCommand command = creationService.map(dependent);
        command.setRetryStrategy(RetryStrategy.BASIC_RETRY);
        commander.createOrder(command);
    }
}
