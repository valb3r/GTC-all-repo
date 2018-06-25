package com.gtc.opportunity.trader.service.statemachine.trade;

import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeEvent;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.BiConsumer;

import static com.gtc.opportunity.trader.domain.TradeEvent.*;

/**
 * Created by Valentyn Berezin on 02.03.18.
 */
@Service
@RequiredArgsConstructor
public class TradeMessageProcessor {

    private final TradeRepository tradeRepository;
    private final CurrentTimestamp timestamp;

    @Transactional
    public Optional<Trade> acceptAndGet(StateContext<TradeStatus, TradeEvent> state,
                                        BiConsumer<Trade, String> processData) {
        Optional<Trade> returnTrade = tradeRepository.findById(state.getStateMachine().getId());
        returnTrade.ifPresent(trade -> {
            header(DATA, state).ifPresent(it -> processData.accept(trade, it));
            header(MSG_ID, state).ifPresent(trade::setLastMessageId);
            header(AMOUNT, BigDecimal.class, state).ifPresent(trade::setAmount);
            header(PRICE, BigDecimal.class, state).ifPresent(trade::setPrice);
            header(STATUS, state).ifPresent(it -> processData.accept(trade, it));
            header(NATIVE_STATUS, state).ifPresent(it -> processData.accept(trade, it));
            trade.setStatusUpdated(timestamp.dbNow());
            trade.setStatus(state.getTarget().getId());
            tradeRepository.save(trade);
        });

        return returnTrade;
    }

    private static Optional<String> header(String header, StateContext<TradeStatus, TradeEvent> state) {
        return header(header, String.class, state);
    }

    private static <T> Optional<T> header(String header, Class<T> clazz, StateContext<TradeStatus, TradeEvent> state) {
        return Optional.ofNullable(state.getMessageHeaders().get(header, clazz));
    }
}
