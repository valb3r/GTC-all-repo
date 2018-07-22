package com.gtc.persistor.app;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by Valentyn Berezin on 01.07.18.
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "com.gtc.persistor.service",
        "com.gtc.persistor.config"
})
public class AppInitializer {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AppInitializer.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}
