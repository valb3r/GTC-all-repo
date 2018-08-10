package com.gtc.tradinggateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.util.concurrent.Executors;

/**
 * Created by Valentyn Berezin on 19.06.18.
 */
@Configuration
@EnableScheduling
public class ScheduledConfig {

    @Value("${app.schedule.poolSize}")
    private int poolSize;

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(poolSize));
    }
}
