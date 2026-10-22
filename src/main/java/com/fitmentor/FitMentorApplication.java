package com.fitmentor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Authentication is JWT only, so the default in-memory user (and the generated password Spring
 * Boot would print in the log) is disabled.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class FitMentorApplication {
    public static void main(String[] args) {
        SpringApplication.run(FitMentorApplication.class, args);
    }
}
