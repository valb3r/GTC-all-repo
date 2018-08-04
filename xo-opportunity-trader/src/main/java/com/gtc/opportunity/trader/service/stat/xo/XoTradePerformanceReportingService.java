package com.gtc.opportunity.trader.service.stat.xo;

import com.google.common.collect.ImmutableSet;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.domain.TradeStatus;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.repository.stat.rejected.XoTradeRejectedStatTotalRepository;
import com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason;
import com.gtc.opportunity.trader.service.stat.TradePerformanceCalculator;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Valentyn Berezin on 25.06.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XoTradePerformanceReportingService {

    private static final String REJECTED_ALL_COUNT = "Custom/Rejected/All";
    private static final String REJECTED_ALL_WITH_ENABL_CONFIG_COUNT = "Custom/Rejected/AllEnabledConfigured";
    private static final String REJECTED_LOW_BAL_COUNT = "Custom/Rejected/LowBalance";
    private static final String REJECTED_GEN_ERR = "Custom/Rejected/GenError";
    private static final String REJECTED_OPT_FAIL = "Custom/Rejected/OptFail";
    private static final String REJECTED_SINGLE_SIDE_LIMIT = "Custom/Rejected/SingleSideLimit";

    private static final String ACCEPTED_UNKNOWN = "Custom/Accepted/Unknown";
    private static final String ACCEPTED_OPEN = "Custom/Accepted/Open";
    private static final String ACCEPTED_CLOSED = "Custom/Accepted/Closed";
    private static final String ACCEPTED_OTHER = "Custom/Accepted/Other";

    private final TradePerformanceCalculator valueCalculator;
    private final XoTradeRejectedStatTotalRepository rejectedStatRepository;
    private final TradeRepository tradeRepository;

    @Trace(dispatcher = true)
    @Transactional(readOnly = true)
    @Scheduled(fixedRateString = "#{${app.schedule.reportTradePerformanceS} * 1000}")
    public void reportPerformance() {
        long all = rejectedStatRepository.rejectedCount();
        NewRelic.recordMetric(REJECTED_ALL_COUNT, all);
        NewRelic.recordMetric(REJECTED_LOW_BAL_COUNT, rejectedStatRepository
                .rejectedCountByLikeReason(Reason.LOW_BAL.getMsg()));
        NewRelic.recordMetric(REJECTED_GEN_ERR, rejectedStatRepository
                .rejectedCountByLikeReason(Reason.GEN_ERR.getMsg()));
        NewRelic.recordMetric(REJECTED_OPT_FAIL, rejectedStatRepository
                .rejectedCountByLikeReason(Reason.OPT_CONSTR_FAIL.getMsg()));
        NewRelic.recordMetric(REJECTED_ALL_WITH_ENABL_CONFIG_COUNT,
                all
                        - rejectedStatRepository.rejectedCountByLikeReason(Reason.NO_CONFIG.getMsg())
                        - rejectedStatRepository.rejectedCountByLikeReason(Reason.DISABLED.getMsg())
        );
        NewRelic.recordMetric(REJECTED_SINGLE_SIDE_LIMIT, rejectedStatRepository
                .rejectedCountByLikeReason(Reason.SIDE_LIMIT.getMsg()));

        NewRelic.recordMetric(ACCEPTED_UNKNOWN, tradeRepository.countAllByStatusEquals(TradeStatus.UNKNOWN));
        NewRelic.recordMetric(ACCEPTED_OPEN, tradeRepository.countAllByStatusEquals(TradeStatus.OPENED));
        NewRelic.recordMetric(ACCEPTED_CLOSED,
                tradeRepository.countAllByStatusEquals(TradeStatus.CLOSED)
                        + tradeRepository.countAllByStatusEquals(TradeStatus.DONE_MAN));
        NewRelic.recordMetric(ACCEPTED_OTHER, tradeRepository.countAllByStatusNotIn(
                ImmutableSet.of(TradeStatus.UNKNOWN, TradeStatus.OPENED, TradeStatus.CLOSED)
        ));

        valueCalculator.reportValueOnGroupedByPair("XO", tradeRepository.findByXoOrderNotNull(), Trade::getXoOrder);
    }
}
