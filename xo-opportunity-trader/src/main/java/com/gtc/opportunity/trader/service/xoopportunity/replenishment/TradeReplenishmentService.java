package com.gtc.opportunity.trader.service.xoopportunity.replenishment;

import com.google.common.collect.ImmutableSet;
import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.model.gateway.command.create.MultiOrderCreateCommand;
import com.gtc.opportunity.trader.domain.AcceptedXoTrade;
import com.gtc.opportunity.trader.service.UuidGenerator;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.gtc.opportunity.trader.service.dto.PreciseReplenishAmountDto;
import com.gtc.opportunity.trader.service.dto.SatisfyReplenishAmountDto;
import com.gtc.opportunity.trader.service.xoopportunity.replenishment.dto.Replenish;
import com.gtc.opportunity.trader.service.xoopportunity.replenishment.precision.PreciseReplenishmentCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashSet;

/**
 * Created by Valentyn Berezin on 02.04.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeReplenishmentService {

    private final ApproximateReplenishment calculator;
    private final PreciseReplenishmentCalculator preciseCalc;
    private final ReplenishTradeCreationService replenisher;
    private final WsGatewayCommander commander;

    @Transactional
    public void replenish(AcceptedXoTrade trade) {

        SatisfyReplenishAmountDto replenish = calculator.calculateReplenishment(trade);
        PreciseReplenishAmountDto precise = preciseCalc.searchPrecisely(replenish);

        CreateOrderCommand buy = replenisher.createTrade(
                new Replenish(precise.getBuyPrice(), precise.getBuyAmount(), precise.getFrom(), false),
                trade
        );
        CreateOrderCommand sell = replenisher.createTrade(
                new Replenish(precise.getSellPrice(), precise.getSellAmount(), precise.getTo(), true),
                trade
        );

        log.info("Creating multi-order command of buy {}, sell {}", buy, sell);
        commander.createOrders(MultiOrderCreateCommand.builder()
                .clientName("multiple")
                .id(UuidGenerator.get())
                // create new HashSet, so we don't expose ImmutableSet for deserialization
                .commands(new HashSet<>(ImmutableSet.of(buy, sell)))
                .build()
        );
    }
}
