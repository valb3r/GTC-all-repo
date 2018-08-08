package com.gtc.opportunity.trader.service.xoopportunity.creation.precision;

import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.service.xoopportunity.creation.fastexception.RejectionException;
import com.gtc.opportunity.trader.service.xoopportunity.creation.precision.dto.XoTradeCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keeps {@link XoTransactionCalculator} warm so it operates normally.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarmupXoTransactionCalculator {

    private final XoTransactionCalculator xoTransactionCalculator;

    @Value("${app.warmup.xoCalculator.considerWarmOnNsuccess}")
    private int considerWarmOnNsuccess;

    @Value("${app.warmup.xoCalculator.solveTimeMs}")
    private int solveTimeMs;

    private final AtomicInteger passes = new AtomicInteger();

    @Scheduled(fixedDelayString = "#{${app.warmup.xoCalculator.scheduleS} * 1000}")
    public void warmupCalculator() {
        if (passes.get() > considerWarmOnNsuccess) {
            return;
        }

        String oldName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName("Warmup XoTransactionCalculator");

            xoTransactionCalculator.calculate(buildCondition());
            passes.incrementAndGet();
        } catch (RejectionException ex) {
            log.info("Cold solver");
            passes.set(0);
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }

    private XoTradeCondition buildCondition() {
        return new XoTradeCondition(
                "TEST", 1000.0,
                0.01, 0.1, 0.02228772013595108, 0.1, 0.044682234, 0.0448677565,
                new BigDecimal("1.01"), new BigDecimal("1.01"),
                new BigDecimal("0.999"), new BigDecimal("0.998"),
                new BigDecimal("0.000001"), new BigDecimal("0.00001"), new BigDecimal("0.001"),
                new BigDecimal("0.00001"), new BigDecimal("1.0009"), solveTimeMs,
                new FullCrossMarketOpportunity.Histogram[]{
                        new FullCrossMarketOpportunity.Histogram(0.044724, 0.044767999999999995, 0.0),
                        new FullCrossMarketOpportunity.Histogram(0.04468, 0.044724, -0.98509921)
                },
                new FullCrossMarketOpportunity.Histogram[]{
                        new FullCrossMarketOpportunity.Histogram(0.0447956, 0.0448328, 1.638),
                        new FullCrossMarketOpportunity.Histogram(0.0448328, 0.04487, 8.75)
                }
        );
    }
}
