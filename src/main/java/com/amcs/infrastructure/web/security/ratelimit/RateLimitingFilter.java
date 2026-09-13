package com.amcs.infrastructure.web.security.ratelimit;

import com.amcs.infrastructure.web.error.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance, in-memory token-bucket rate limiter.
 * Protects critical endpoints from credential brute-forcing and resource exhaustion:
 * 1. Authentication (POST /api/v1/auth/login)
 * 2. Heavy bulk imports (POST /api/v1/imports/**)
 * 3. Heavy report generation (GET /api/v1/reports/**)
 */
@Component
@Order(-100)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final boolean enabled;
    private final int authLimitPerMinute;
    private final int heavyLimitPerMinute;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger requestCounter = new AtomicInteger(0);

    public RateLimitingFilter(
        @Value("${amcs.security.ratelimit.enabled:true}") boolean enabled,
        @Value("${amcs.security.ratelimit.auth-limit-per-minute:10}") int authLimitPerMinute,
        @Value("${amcs.security.ratelimit.heavy-limit-per-minute:25}") int heavyLimitPerMinute,
        ObjectMapper objectMapper
    ) {
        this.enabled = enabled;
        this.authLimitPerMinute = authLimitPerMinute;
        this.heavyLimitPerMinute = heavyLimitPerMinute;
        this.objectMapper = objectMapper != null 
            ? objectMapper.copy().findAndRegisterModules() 
            : new ObjectMapper().findAndRegisterModules();
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        BucketConfig config = determineConfig(method, path);
        if (config == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Periodic maintenance cleanup every 500 requests
        if ((requestCounter.incrementAndGet() & 511) == 0) {
            cleanupStaleBuckets();
        }

        String clientKey = extractClientKey(request);
        String bucketKey = config.prefix() + ":" + clientKey;

        long now = System.currentTimeMillis();
        TokenBucket bucket = buckets.compute(bucketKey, (k, existing) -> {
            if (existing == null) {
                return new TokenBucket(config.capacity(), config.refillPerMinute(), now);
            }
            existing.refill(now);
            return existing;
        });

        if (bucket.tryConsume()) {
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = Math.max(1, bucket.secondsUntilNextToken(now));
            log.warn("Rate limit exceeded for client [{}] on [{}] [{}]. Retry after {}s",
                clientKey, method, path, retryAfterSeconds);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

            ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "RATE_LIMIT_EXCEEDED",
                "Rate limit exceeded. Please retry after " + retryAfterSeconds + " seconds.",
                path
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
        }
    }

    private BucketConfig determineConfig(String method, String path) {
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/v1/auth/login")) {
            return new BucketConfig("auth", authLimitPerMinute, authLimitPerMinute);
        }
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/v1/imports")) {
            return new BucketConfig("import", heavyLimitPerMinute, heavyLimitPerMinute);
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/v1/reports")) {
            return new BucketConfig("report", heavyLimitPerMinute, heavyLimitPerMinute);
        }
        return null;
    }

    private String extractClientKey(HttpServletRequest request) {
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void cleanupStaleBuckets() {
        long threshold = System.currentTimeMillis() - 300_000L; // 5 minutes idle
        buckets.entrySet().removeIf(entry -> entry.getValue().lastRefillTimestamp < threshold);
    }

    // Visible for testing to reset state
    public void reset() {
        buckets.clear();
    }

    private record BucketConfig(String prefix, int capacity, int refillPerMinute) {}

    private static class TokenBucket {
        private final int capacity;
        private final double tokensPerMillisecond;
        private double tokens;
        private long lastRefillTimestamp;

        TokenBucket(int capacity, int refillPerMinute, long now) {
            this.capacity = capacity;
            this.tokensPerMillisecond = (double) refillPerMinute / 60_000.0;
            this.tokens = capacity;
            this.lastRefillTimestamp = now;
        }

        synchronized void refill(long now) {
            long elapsed = Math.max(0, now - lastRefillTimestamp);
            double added = elapsed * tokensPerMillisecond;
            tokens = Math.min(capacity, tokens + added);
            lastRefillTimestamp = now;
        }

        synchronized boolean tryConsume() {
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        synchronized long secondsUntilNextToken(long now) {
            if (tokens >= 1.0) return 0;
            double needed = 1.0 - tokens;
            long millis = (long) Math.ceil(needed / tokensPerMillisecond);
            return Math.max(1, (millis + 999) / 1000);
        }
    }
}
