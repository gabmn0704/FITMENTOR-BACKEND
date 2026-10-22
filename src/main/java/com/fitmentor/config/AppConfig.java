package com.fitmentor.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/** Shared infrastructure beans and scheduling (idle WebSocket session sweep). */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(FitmentorProperties.class)
public class AppConfig {

    /** UTC clock, injected so time-based rules (rate limit, streaks, expiry) are testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
