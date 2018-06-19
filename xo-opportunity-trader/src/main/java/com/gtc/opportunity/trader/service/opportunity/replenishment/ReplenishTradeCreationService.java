package com.gtc.opportunity.trader.service.opportunity.replenishment;

import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.opportunity.trader.domain.AcceptedXoTrade;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.dto.TradeDto;
import com.gtc.opportunity.trader.service.opportunity.common.TradeCreationService;
import com.gtc.opportunity.trader.service.opportunity.replenishment.dto.Replenish;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

/**
 * Created by Valentyn Berezin on 02.04.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplenishTradeCreationService {

    private final TradeCreationService creationService;
    private final TradeRepository tradeRepository;

    @Transactional
    public CreateOrderCommand createTrade(Replenish replenish, AcceptedXoTrade xoTrade) {
        log.info("Creating trade for replenishing {}", replenish);
        TradeDto trade = creationService
                .createTrade(replenish.getCfg(), replenish.getPrice(), replenish.getAmount(), replenish.isSell());
        trade.getTrade().setXoOrder(xoTrade);
        tradeRepository.save(trade.getTrade());
        return trade.getCommand();
    }
}
