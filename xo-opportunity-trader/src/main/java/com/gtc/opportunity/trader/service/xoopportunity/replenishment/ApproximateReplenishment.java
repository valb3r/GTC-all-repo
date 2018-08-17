package com.gtc.opportunity.trader.service.xoopportunity.replenishment;

import com.gtc.opportunity.trader.domain.AcceptedXoTrade;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.dto.SatisfyReplenishAmountDto;
import com.gtc.opportunity.trader.service.xoopportunity.creation.ConfigCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Created by Valentyn Berezin on 04.04.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproximateReplenishment {

    private static final Supplier<RuntimeException> NO_CNF = () -> new IllegalStateException("No config active");

    private final ConfigCache configCache;
    private final TradeRepository tradeRepository;

    @Transactional
    public SatisfyReplenishAmountDto calculateReplenishment(AcceptedXoTrade trade) {
        log.info("Calculate approximate replenisment for trade {}", trade);
        Collection<Trade> trades = tradeRepository.findByXoOrderId(trade.getId());
        Trade oldSell = oldSell(trades);
        Trade oldBuy = oldBuy(trades);
        BigDecimal keepAmount = trade.getExpectedProfit();
        ClientConfig from = from(trade);
        ClientConfig to = to(trade);


        return new SatisfyReplenishAmountDto(
                oldBuy.getOpeningPrice(),
                oldSell.getOpeningPrice(),
                oldBuyReplenish(oldBuy, keepAmount.doubleValue()),
                oldSellReplenish(oldSell, from),
                from,
                to
        );
    }

    private double oldSellReplenish(Trade oldSell, ClientConfig from) {
        return oldSell.getOpeningAmount().abs().doubleValue() / getCharge(from).doubleValue();
    }

    private double oldBuyReplenish(Trade trade, double keepAmount) {
        return trade.getExpectedReverseAmount().abs().doubleValue() - keepAmount;
    }

    private BigDecimal getCharge(ClientConfig from) {
        return BigDecimal.ONE.subtract(from.getTradeChargeRatePct().movePointLeft(2));
    }

    private Trade oldSell(Collection<Trade> trades) {
        return trades.stream().filter(Trade::isSell).findFirst()
                .orElseThrow(() -> new IllegalStateException("No sell order"));
    }

    private Trade oldBuy(Collection<Trade> trades) {
        return trades.stream().filter(it -> !it.isSell()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No buy order"));
    }

    private ClientConfig from(AcceptedXoTrade trade) {
        return configCache.getClientCfg(trade.getClientFrom().getName(),
                trade.getCurrencyFrom(),
                trade.getCurrencyTo()
        ).orElseThrow(NO_CNF);
    }

    private ClientConfig to(AcceptedXoTrade trade) {
        return configCache.getClientCfg(trade.getClientTo().getName(),
                trade.getCurrencyFrom(),
                trade.getCurrencyTo()
        ).orElseThrow(NO_CNF);
    }
}
