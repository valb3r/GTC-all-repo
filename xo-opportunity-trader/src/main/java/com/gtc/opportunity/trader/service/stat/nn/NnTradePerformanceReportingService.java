package com.gtc.opportunity.trader.service.stat.nn;

import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.repository.TradeRepository;
import com.gtc.opportunity.trader.service.nnopportunity.repository.ContainerStatistics;
import com.gtc.opportunity.trader.service.nnopportunity.repository.NnDataRepository;
import com.gtc.opportunity.trader.service.nnopportunity.solver.NnSolver;
import com.gtc.opportunity.trader.service.stat.TradePerformanceCalculator;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by Valentyn Berezin on 25.06.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NnTradePerformanceReportingService {

    private static final String MODEL_AGE_50 = "Custom/NN/Model/Max/AgeFiftyPercentileS";
    private static final String MODEL_AGE_75 = "Custom/NN/Model/Max/AgeSeventyFivePercentileS";
    private static final String MODEL_AGE_90 = "Custom/NN/Model/Max/AgeNinetyPercentileS";
    private static final String NOOP_AGE = "Custom/NN/Label/Noop/Max/AgeS";
    private static final String ACT_AGE = "Custom/NN/Label/Act/Max/AgeS";
    private static final String CACHE_FULLNESS = "Custom/NN/Cache/Fullness/Min";
    private static final String LABEL_FULLNESS = "Custom/NN/Label/Fullness/Min";

    private final NnSolver solver;
    private final NnDataRepository dataRepository;
    private final TradePerformanceCalculator performanceCalculator;
    private final TradeRepository tradeRepository;

    @Trace(dispatcher = true)
    @Transactional(readOnly = true)
    @Scheduled(fixedRateString = "#{${app.schedule.reportTradePerformanceS} * 1000}")
    public void reportPerformance() {
        long currTime = System.currentTimeMillis();

        List<ContainerStatistics> stats = dataRepository.getStatistics(currTime);
        double maxNoopAgeS = stats.stream().mapToDouble(ContainerStatistics::getAvgNoopAgeS).max().orElse(0.0);
        double maxActAgeS = stats.stream().mapToDouble(ContainerStatistics::getAvgActAgeS).max().orElse(0.0);
        double minCacheFull = stats.stream().mapToDouble(ContainerStatistics::getMinCacheFullness).min().orElse(0.0);
        double minLabelFull = stats.stream().mapToDouble(ContainerStatistics::getMinLabelFullness).min().orElse(0.0);

        NnSolver.ModelStatistics statistics = solver.ageStats(currTime);
        NewRelic.recordMetric(MODEL_AGE_50, (float) statistics.getAgePercentile50());
        NewRelic.recordMetric(MODEL_AGE_75, (float) statistics.getAgePercentile75());
        NewRelic.recordMetric(MODEL_AGE_90, (float) statistics.getAgePercentile90());
        NewRelic.recordMetric(NOOP_AGE, (float) maxNoopAgeS);
        NewRelic.recordMetric(ACT_AGE, (float) maxActAgeS);
        NewRelic.recordMetric(CACHE_FULLNESS, (float) minCacheFull);
        NewRelic.recordMetric(LABEL_FULLNESS, (float) minLabelFull);

        TradePerformanceCalculator.Performance performance =
                performanceCalculator.calculateOnGroupedByPair(tradeRepository.findByNnOrderNotNull(), Trade::getNnOrder);
        performanceCalculator.reportPerformance("NN", performance);
    }
}
