package org.openfamilycompass.security;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.openfamilycompass.api.v1.dto.ApiErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple in-memory rate limiting filter for authentication endpoints.
 * <p>
 * Limits requests per client IP address on the stateless JWT auth endpoints to
 * mitigate brute-force attacks against {@code /api/v1/auth/login} and token
 * refresh abuse against {@code /api/v1/auth/refresh}.
 * <p>
 * Limits are configurable via:
 * <ul>
 *   <li>{@code app.security.rate-limit.auth.max-requests-per-minute}
 *       (default: {@value #DEFAULT_MAX_REQUESTS_PER_MINUTE})</li>
 *   <li>{@code app.security.rate-limit.auth.window-size-ms}
 *       (default: {@value #DEFAULT_WINDOW_SIZE_MS})</li>
 * </ul>
 * <p>
 * This implementation is per-instance (not cluster-wide). For multi-instance
 * deployments consider backing the counter store with Redis.
 */
@Component
public class RateLimitingFilter implements Filter {

    static final int DEFAULT_MAX_REQUESTS_PER_MINUTE = 10;
    static final long DEFAULT_WINDOW_SIZE_MS = 60_000L;

    /** Exact URI paths that are subject to rate limiting. */
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh");

    /** Clear out stale counters once the map exceeds this size. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int maxRequestsPerMinute;
    private final long windowSizeMs;
    private final ObjectMapper objectMapper;

    // Map: client IP -> Request counter
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${app.security.rate-limit.auth.max-requests-per-minute:"
                    + DEFAULT_MAX_REQUESTS_PER_MINUTE + "}") int maxRequestsPerMinute,
            @Value("${app.security.rate-limit.auth.window-size-ms:"
                    + DEFAULT_WINDOW_SIZE_MS + "}") long windowSizeMs,
            ObjectMapper objectMapper) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.windowSizeMs = windowSizeMs;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Apply rate limiting only to authentication endpoints
        if (shouldRateLimit(path)) {
            String clientIp = getClientIp(httpRequest);

            if (isRateLimited(clientIp)) {
                writeTooManyRequestsResponse(httpResponse);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean shouldRateLimit(String path) {
        return RATE_LIMITED_PATHS.contains(path);
    }

    private boolean isRateLimited(String clientIp) {
        RequestCounter counter = requestCounts.computeIfAbsent(clientIp, k -> new RequestCounter());

        long now = System.currentTimeMillis();

        // Reset counter if window expired
        if (now - counter.windowStart > windowSizeMs) {
            counter.reset(now);
        }

        // Increment and check
        int currentCount = counter.count.incrementAndGet();

        // Clean up old entries periodically to bound memory
        if (requestCounts.size() > CLEANUP_THRESHOLD) {
            cleanupOldEntries(now);
        }

        return currentCount > maxRequestsPerMinute;
    }

    private void cleanupOldEntries(long now) {
        requestCounts.entrySet().removeIf(entry -> now - entry.getValue().windowStart > windowSizeMs);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequestsResponse(HttpServletResponse httpResponse) throws IOException {
        httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpResponse.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(windowSizeMs / 1000L));

        ApiErrorResponse body = ApiErrorResponse.builder()
                .error("TOO_MANY_REQUESTS")
                .message("Too many requests. Please try again later.")
                .build();
        objectMapper.writeValue(httpResponse.getWriter(), body);
    }

    private static class RequestCounter {
        final AtomicInteger count = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();

        void reset(long now) {
            count.set(0);
            windowStart = now;
        }
    }
}
