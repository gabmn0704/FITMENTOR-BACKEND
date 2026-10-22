package com.fitmentor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Typed configuration bound from the {@code fitmentor.*} properties, which are fed by
 * environment variables (JWT_SECRET, GOOGLE_CLIENT_ID, FRONTEND_ORIGIN, RATE_LIMIT_*).
 */
@ConfigurationProperties(prefix = "fitmentor")
public record FitmentorProperties(Jwt jwt, Google google, Cors cors, RateLimit rateLimit) {

    public FitmentorProperties {
        jwt = jwt == null ? new Jwt(null) : jwt;
        google = google == null ? new Google(null) : google;
        cors = cors == null ? new Cors(List.of("*")) : cors;
        rateLimit = rateLimit == null ? new RateLimit(0, 0) : rateLimit;
    }

    /** Requests per minute and client for login and coach; 0 or less uses the default. */
    public record RateLimit(int authPerMinute, int coachPerMinute) {
        public RateLimit {
            authPerMinute = authPerMinute > 0 ? authPerMinute : 20;
            coachPerMinute = coachPerMinute > 0 ? coachPerMinute : 30;
        }
    }

    /** Signing secret for HS256 tokens. Blank means "not configured". */
    public record Jwt(String secret) {
    }

    /** OAuth client id that Google ID tokens must be issued for. */
    public record Google(String clientId) {
    }

    /** Allowed browser origins; "*" allows any origin. */
    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null || allowedOrigins.isEmpty() ? List.of("*") : List.copyOf(allowedOrigins);
        }
    }
}
