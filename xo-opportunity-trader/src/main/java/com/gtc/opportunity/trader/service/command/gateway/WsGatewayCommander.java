package com.gtc.opportunity.trader.service.command.gateway;

import com.gtc.model.gateway.command.account.GetAllBalancesCommand;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.command.create.MultiOrderCreateCommand;
import com.gtc.model.gateway.command.manage.GetOrderCommand;
import com.gtc.model.gateway.command.manage.ListOpenCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.Valid;

/**
 * Created by Valentyn Berezin on 23.02.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsGatewayCommander {

    private final WsGatewayClient client;

    public void createOrder(@Valid CreateOrderCommand command) {
        log.info("Requesting to create order {}", command);
        client.sendCommand(command);
    }

    public void createOrders(@Valid MultiOrderCreateCommand command) {
        log.info("Requesting to create composite order {}", command);
        client.sendCommand(command);
    }

    public void listOpenOrders(@Valid ListOpenCommand command) {
        log.info("Requesting to list open orders {}", command);
        client.sendCommand(command);
    }

    public void getOrder(@Valid GetOrderCommand command) {
        log.info("Requesting to get order {}", command);
        client.sendCommand(command);
    }

    public void getBalances(@Valid GetAllBalancesCommand command) {
        log.info("Requesting to get balances {}", command);
        client.sendCommand(command);
    }
}
