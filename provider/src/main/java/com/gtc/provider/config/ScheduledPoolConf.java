package com.gtc.provider.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.util.concurrent.Executors;

import static com.gtc.provider.config.Const.CONF_ROOT_SCHEDULE_CHILD;

/**
 * Created by Valentyn Berezin on 07.01.18.
 */
@Configuration
public class ScheduledPoolConf {

    private static final String SPEL_POOL = "${" + CONF_ROOT_SCHEDULE_CHILD + "pool}";

    @Value(SPEL_POOL)
    private int poolSz;

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(poolSz));
    }
}
