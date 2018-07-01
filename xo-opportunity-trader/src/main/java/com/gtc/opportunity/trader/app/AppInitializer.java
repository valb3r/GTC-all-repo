package com.gtc.opportunity.trader.app;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@EnableRetry
@EnableScheduling
@EnableTransactionManagement
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EntityScan("com.gtc.opportunity.trader.domain")
@EnableJpaRepositories(basePackages = {
        "com.gtc.opportunity.trader.repository"
})
@SpringBootApplication(scanBasePackages = {
        "com.gtc.opportunity.trader.aop",
        "com.gtc.opportunity.trader.service",
        "com.gtc.opportunity.trader.config"
})
public class AppInitializer {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AppInitializer.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}

