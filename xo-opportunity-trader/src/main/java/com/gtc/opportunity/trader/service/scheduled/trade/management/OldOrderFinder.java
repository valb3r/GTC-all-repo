package com.gtc.opportunity.trader.service.scheduled.trade.management;

import com.google.common.collect.ImmutableList;
import com.gtc.meta.TradingCurrency;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.repository.NnConfigRepository;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.gtc.opportunity.trader.domain.TradeStatus.OPENED;

/**
 * Created by Valentyn Berezin on 03.09.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OldOrderFinder {

    private final TradeRepository tradeRepository;
    private final CurrentTimestamp dbTime;
    private final NnConfigRepository nnCfg;

    public List<Trade> expiredMaster(LocalDateTime now, Map<Key, Integer> expiryM) {
        return listOld(
                (delayM, trade) -> canCancelMaster(delayM, now, trade),
                expiryM,
                atLeastM -> tradeRepository
                        .findNnMasterByStatusInAndRecordedOnBefore(
                                ImmutableList.of(OPENED), now.minusMinutes(atLeastM), true)
        );
    }

    public List<Trade> expiredSlave(LocalDateTime now, Map<Key, Integer> expiryM) {
        return listOld(
                (delayM, trade) -> canCancelSlave(delayM, now, trade),
                expiryM,
                atLeastM -> tradeRepository.findNnSlaveByStatusInAndStatusUpdatedBefore(
                        ImmutableList.of(OPENED), now.minusMinutes(atLeastM), true)
        );
    }

    private List<Trade> listOld(BiFunction<Integer, Trade, Boolean> filter, Map<Key, Integer> expiry,
                                Function<Integer, List<Trade>> listExpired) {
        OptionalInt minMinutes = expiry.values().stream().mapToInt(it -> it).min();

        if (!minMinutes.isPresent()) {
            return Collections.emptyList();
        }

        List<Trade> suspectedExpired = listExpired.apply(minMinutes.getAsInt());
        return suspectedExpired.stream()
                .filter(it -> filter.apply(expiry.get(key(it)), it))
                .collect(Collectors.toList());
    }

    private boolean canCancelMaster(Integer delayM, LocalDateTime now, Trade trade) {
        return delayM != null && trade.getRecordedOn().plusMinutes(delayM).compareTo(now) < 0;
    }

    private boolean canCancelSlave(Integer delayM, LocalDateTime now, Trade trade) {
        return delayM != null && trade.getStatusUpdated().plusMinutes(delayM).compareTo(now) < 0;
    }

    private static Key key(Trade trade) {
        return new Key(
                trade.getClient().getName(),
                trade.getCurrencyFrom(),
                trade.getCurrencyTo()
        );
    }

    @Data
    public static class Key {

        private final String clientName;
        private final TradingCurrency from;
        private final TradingCurrency to;
    }
}
