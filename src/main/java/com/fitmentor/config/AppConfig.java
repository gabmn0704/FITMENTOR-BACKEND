package com.fitmentor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Shared infrastructure beans. */
@Configuration
public class AppConfig {

    /** UTC clock, injected so time-based rules (rate limit, streaks, expiry) are testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
