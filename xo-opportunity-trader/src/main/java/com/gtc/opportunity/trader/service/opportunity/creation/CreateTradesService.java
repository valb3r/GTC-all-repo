package com.gtc.opportunity.trader.service.opportunity.creation;

import com.google.common.collect.ImmutableSet;
import com.gtc.model.gateway.command.create.MultiOrderCreateCommand;
import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.domain.AcceptedXoTrade;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.XoAcceptStatus;
import com.gtc.opportunity.trader.repository.AcceptedXoTradeRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.UuidGenerator;
import com.gtc.opportunity.trader.service.command.gateway.WsGatewayCommander;
import com.gtc.opportunity.trader.service.dto.PreciseXoAmountDto;
import com.gtc.opportunity.trader.service.dto.TradeDto;
import com.gtc.opportunity.trader.service.opportunity.common.TradeCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.HashSet;

/**
 * Created by Valentyn Berezin on 02.04.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateTradesService {

    private final TradeRepository tradeRepository;
    private final TradeCreationService trades;
    private final AcceptedXoTradeRepository xoTradeRepository;
    private final OpportunityMapperFactory mapperFactory;
    private final WsGatewayCommander commander;

    @Transactional
    public void createTrades(FullCrossMarketOpportunity opp, PreciseXoAmountDto amount, ClientConfig cfgFrom,
                             ClientConfig cfgTo) {
        AcceptedXoTrade xoTrade = buildXo(opp, amount, cfgFrom, cfgTo);

        TradeDto from = trades.createTrade(cfgFrom, amount.getSellPrice(), amount.getSellAmount(), true);
        TradeDto to = trades.createTrade(cfgTo, amount.getBuyPrice(), amount.getBuyAmount(), false);
        log.info("Creating commands for {} as {} to {}", opp, from, to);
        xoTrade = xoTradeRepository.save(xoTrade);
        from.getTrade().setXoOrder(xoTrade);
        to.getTrade().setXoOrder(xoTrade);
        tradeRepository.save(from.getTrade());
        tradeRepository.save(to.getTrade());

        commander.createOrders(MultiOrderCreateCommand.builder()
                .clientName("multiple")
                .id(UuidGenerator.get())
                // create new HashSet, so we don't expose ImmutableSet for deserialization
                .commands(new HashSet<>(ImmutableSet.of(from.getCommand(), to.getCommand())))
                .build()
        );
    }

    private AcceptedXoTrade buildXo(FullCrossMarketOpportunity xoOpp, PreciseXoAmountDto amount, ClientConfig cfgFrom,
                                    ClientConfig cfgTo) {
        OpportunityMapperFactory.MappedOpp opp = mapperFactory.map(xoOpp, cfgFrom, cfgTo);

        return AcceptedXoTrade.builder()
                .clientFrom(cfgFrom.getClient())
                .clientTo(cfgTo.getClient())
                .currencyFrom(cfgFrom.getCurrency())
                .currencyTo(cfgTo.getCurrencyTo())
                .amount(amount.getSellAmount())
                .priceFromBuy(amount.getBuyPrice())
                .priceToSell(amount.getSellPrice())
                .expectedProfit(amount.getProfit())
                .expectedProfitPct(BigDecimal.valueOf(amount.getProfitPct()))
                .lastMessageId("OP-" + xoOpp.getId())
                .status(XoAcceptStatus.UNCONFIRMED)
                .opportunityOpenedOn(xoOpp.getOpenedOn())
                .opportunityBestSellAmount(opp.marketToBestSellAmount())
                .opportunityBestBuyAmount(opp.marketFromBestBuyAmount())
                .opportunityBestSellPrice(opp.marketToBestSellPrice())
                .opportunityBestBuyPrice(opp.marketFromBestBuyPrice())
                .opportunityProfitPct(opp.profitPct())
                .build();
    }
}
