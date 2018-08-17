package com.gtc.opportunity.trader.service.xoopportunity.creation;

import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.repository.AcceptedXoTradeRepository;
import com.gtc.opportunity.trader.service.CurrentTimestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;

/**
 * Created by Valentyn Berezin on 25.02.18.
 */
@Service
@RequiredArgsConstructor
public class RateCheckingService {

    private final CurrentTimestamp currentTimestamp;
    private final AcceptedXoTradeRepository tradeRepository;

    @Transactional(readOnly = true)
    public boolean ratePass(ClientConfig from, ClientConfig to) {
        double rate = Math.min(from.getXoConfig().getXoRatePerSec(), to.getXoConfig().getXoRatePerSec());
        long windowMs = Math.round(1000.0 / rate);
        int cnt = tradeRepository.countByKeyOlderThan(
                from.getClient().getName(),
                to.getClient().getName(),
                from.getCurrency(),
                from.getCurrencyTo(),
                currentTimestamp.dbNow().minus(windowMs, ChronoUnit.MILLIS)
        );

        return cnt == 0;
    }
}
