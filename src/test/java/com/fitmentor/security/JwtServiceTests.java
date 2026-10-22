package com.fitmentor.security;

import com.fitmentor.config.FitmentorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTests {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-21T12:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };

    @Test
    void issuedTokenIsVerifiedToTheSameUser() {
        JwtService jwt = service(SECRET, new MockEnvironment());
        UUID id = UUID.randomUUID();

        AuthenticatedUser user = jwt.verify(jwt.issue(id, true)).orElseThrow();

        assertThat(user).isEqualTo(new AuthenticatedUser(id, true));
    }

    @Test
    void tokenExpiresAfterSevenDays() {
        JwtService jwt = service(SECRET, new MockEnvironment());
        String token = jwt.issue(UUID.randomUUID(), false);

        now.set(now.get().plus(Duration.ofDays(6)));
        assertThat(jwt.verify(token)).isPresent();

        now.set(now.get().plus(Duration.ofDays(2)));
        assertThat(jwt.verify(token)).isEmpty();
    }

    @Test
    void tamperedOrForeignTokensAreRejected() {
        JwtService jwt = service(SECRET, new MockEnvironment());
        JwtService other = service("ffffffffffffffffffffffffffffffff", new MockEnvironment());
        String token = jwt.issue(UUID.randomUUID(), false);

        assertThat(jwt.verify(token + "x")).isEmpty();
        assertThat(other.verify(token)).isEmpty();
        assertThat(jwt.verify("not-a-jwt")).isEmpty();
        assertThat(jwt.verify(null)).isEmpty();
    }

    @Test
    void shortSecretIsRejectedAtStartup() {
        assertThatThrownBy(() -> service("too-short", new MockEnvironment()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32 characters");
    }

    @Test
    void productionRequiresSecret() {
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("render");

        assertThatThrownBy(() -> service("", production))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET is required");
    }

    @Test
    void developmentGeneratesEphemeralSecret() {
        JwtService jwt = service("", new MockEnvironment());

        assertThat(jwt.verify(jwt.issue(UUID.randomUUID(), true))).isPresent();
    }

    private JwtService service(String secret, MockEnvironment environment) {
        return new JwtService(new FitmentorProperties(new FitmentorProperties.Jwt(secret), null, null, null), environment, clock);
    }
}
