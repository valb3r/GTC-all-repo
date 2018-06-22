package com.gtc.opportunity.trader.service.dto;

import com.gtc.model.gateway.command.create.CreateOrderCommand;
import com.gtc.opportunity.trader.domain.Trade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Created by Valentyn Berezin on 28.02.18.
 */
@Getter
@ToString
@AllArgsConstructor
public final class TradeDto {

    private final Trade trade;
    private final CreateOrderCommand command;
}
