package com.gtc.opportunity.trader.service.statemachine.nnaccept;

import com.gtc.model.gateway.RetryStrategy;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.gtc.opportunity.trader.service.xoopportunity.common.TradeCreationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Valentyn Berezin on 08.08.18.
 */
@Service
@RequiredArgsConstructor
public class NnDependencyHandler {

    private final TradeCreationService creationService;
    private final WsGatewayCommander commander;
    private final NnDependencyPropagator dependencyPropagator;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void dependencyDone(Trade dependent) {
        dependencyPropagator.ackDependencyDone(dependent.getId());
        CreateOrderCommand command = creationService.map(dependent);
        command.setRetryStrategy(RetryStrategy.BASIC_RETRY);
        commander.createOrder(command);
    }
}
