package com.gtc.tradinggateway.app;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages = {
        "com.gtc.tradinggateway.aspect",
        "com.gtc.tradinggateway.service",
        "com.gtc.tradinggateway.controller",
        "com.gtc.tradinggateway.config"
}, exclude = ActiveMQAutoConfiguration.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AppInitializer {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AppInitializer.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}

