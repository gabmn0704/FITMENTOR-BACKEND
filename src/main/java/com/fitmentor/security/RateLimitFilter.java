package com.fitmentor.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitmentor.api.error.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client fixed-window rate limit for the endpoints that are cheap to abuse: login
 * ({@code POST /api/auth/**}: guest creation, registration, Google and password login) and the coach ({@code POST /api/coach},
 * which may call a paid model). The client is the remote address; on Render it is the real
 * client IP because {@code server.forward-headers-strategy=native} trusts only the private proxy.
 * Over the limit the answer is 429 with the usual {@link ApiError} body.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    static final Duration WINDOW = Duration.ofMinutes(1);
    static final int MAX_TRACKED_CLIENTS = 50_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final int authLimit;
    private final int coachLimit;

    /** Limits are requests per minute and per client (see {@code fitmentor.rate-limit.*}). */
    public RateLimitFilter(ObjectMapper objectMapper, Clock clock, int authLimit, int coachLimit) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.authLimit = authLimit;
        this.coachLimit = coachLimit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return limitFor(request) == 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        int limit = limitFor(request);
        String key = bucket(request) + '|' + request.getRemoteAddr();
        long now = clock.millis();
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(window -> window.expired(now));
        }
        Window window = windows.compute(key, (k, current) ->
            current == null || current.expired(now) ? new Window(now + WINDOW.toMillis(), 1) : current.increment());
        if (window.count() > limit) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(Math.max(1, (window.resetAt() - now) / 1000)));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                new ApiError("rate_limited", "Too many requests. Try again in a minute."));
            return;
        }
        chain.doFilter(request, response);
    }

    private int limitFor(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return 0;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            return authLimit;
        }
        if (path.equals("/api/coach")) {
            return coachLimit;
        }
        return 0;
    }

    private static String bucket(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/auth/") ? "auth" : "coach";
    }

    private record Window(long resetAt, int count) {
        boolean expired(long now) {
            return now >= resetAt;
        }

        Window increment() {
            return new Window(resetAt, count + 1);
        }
    }
}
