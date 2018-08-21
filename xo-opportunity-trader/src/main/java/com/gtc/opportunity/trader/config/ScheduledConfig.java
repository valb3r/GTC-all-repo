package com.gtc.opportunity.trader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.util.concurrent.Executors;

import static com.gtc.opportunity.trader.config.ScheduledConfig.NO_SCHEDULER;

/**
 * Created by Valentyn Berezin on 19.06.18.
 */
@Profile("!" + NO_SCHEDULER)
@Configuration
@EnableScheduling
public class ScheduledConfig {

    public static final String NO_SCHEDULER = "no_scheduler";

    @Value("${app.schedule.poolSize}")
    private int poolSize;
    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(poolSize));
    }
}
