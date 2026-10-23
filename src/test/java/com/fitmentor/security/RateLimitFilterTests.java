package com.fitmentor.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTests {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-21T12:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };
    private final RateLimitFilter filter = new RateLimitFilter(new ObjectMapper(), clock, 3, 2);

    @Test
    void guestLoginIsLimitedPerClientAndWindow() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(call("POST", "/api/auth/guest", "10.0.0.1").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse blocked = call("POST", "/api/auth/guest", "10.0.0.1");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentAsString()).contains("\"error\":\"rate_limited\"");
        assertThat(blocked.getHeader("Retry-After")).isNotBlank();

        assertThat(call("POST", "/api/auth/guest", "10.0.0.2").getStatus()).isEqualTo(200);
        now.set(now.get().plus(RateLimitFilter.WINDOW));
        assertThat(call("POST", "/api/auth/guest", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void coachHasItsOwnBudgetAndOtherEndpointsAreNotLimited() throws Exception {
        call("POST", "/api/coach", "10.0.0.3");
        call("POST", "/api/coach", "10.0.0.3");
        assertThat(call("POST", "/api/coach", "10.0.0.3").getStatus()).isEqualTo(429);
        assertThat(call("POST", "/api/auth/guest", "10.0.0.3").getStatus()).isEqualTo(200);
        for (int i = 0; i < 10; i++) {
            assertThat(call("GET", "/api/stats", "10.0.0.3").getStatus()).isEqualTo(200);
            assertThat(call("POST", "/api/sessions", "10.0.0.3").getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse call(String method, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
