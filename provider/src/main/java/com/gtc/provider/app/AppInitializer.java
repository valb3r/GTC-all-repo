package com.gtc.provider.app;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "com.gtc.provider.service",
        "com.gtc.provider.clients",
        "com.gtc.provider.market",
        "com.gtc.provider.controller",
        "com.gtc.provider.config"
})
public class AppInitializer {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AppInitializer.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}

