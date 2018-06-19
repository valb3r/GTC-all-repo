package com.gtc.tradinggateway.aspect.statistics;

import com.gtc.model.gateway.BaseMessage;
import com.newrelic.api.agent.NewRelic;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.gtc.tradinggateway.aspect.statistics.LatencyStatistics.PREFIX;
import static com.gtc.tradinggateway.config.Const.CONF_ROOT_CHILD;
import static com.gtc.tradinggateway.config.Const.STATISTICS;

/**
 * Created by Valentyn Berezin on 31.03.18.
 */
@Slf4j
@Aspect
@Component
@ConditionalOnExpression(value = "${" + PREFIX + ".enabled}")
public class LatencyStatistics {

    static final String PREFIX = CONF_ROOT_CHILD + STATISTICS;
    private final Map<String, DescriptiveStatistics> statistics = new ConcurrentHashMap<>();

    @Value("${" + PREFIX + ".window}")
    private int statWindow;

    @Around("execution(public * com.gtc.tradinggateway.service.command.WsCommandHandler.*(..))")
    public Object getLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        Arrays.stream(joinPoint.getArgs())
                .filter(it -> it instanceof BaseMessage)
                .findFirst()
                .map(it -> (BaseMessage) it)
                .ifPresent(it -> calculateStats(it.getClass().getSimpleName(), it.getCreatedTimestamp()));

        return joinPoint.proceed();
    }

    @Scheduled(fixedDelayString = "${" + PREFIX + ".reportIntervalMs}")
    public void reportStats() {
        log.info("JMS latency 90th percentile, ms {}", percentile(90.0));
        log.info("JMS latency 95th percentile, ms {}", percentile(95.0));
        log.info("JMS latency 99th percentile, ms {}", percentile(99.0));
        log.info("JMS latency 99.9th percentile, ms {}", percentile(99.9));
    }

    private void calculateStats(String queue, long tstmp) {

        statistics
                .computeIfAbsent(queue, id -> new DescriptiveStatistics(statWindow))
                .addValue((double) System.currentTimeMillis() - tstmp);
    }

    private Map<String, Long> percentile(double percentile) {
        return statistics.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, it -> {
                    long latency = (long) it.getValue().getPercentile(percentile);
                    NewRelic.recordMetric("Latency " + it.getKey(), latency);
                    return latency;
                }));
    }
}
