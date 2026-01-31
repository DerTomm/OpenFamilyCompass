package org.openfamilycompass.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple rate limiting filter for authentication endpoints.
 * Limits requests per IP address to prevent brute force attacks.
 */
@Component
public class RateLimitingFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_SIZE_MS = 60_000; // 1 minute

    // Map: IP -> Request count
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

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
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.getWriter().write("Too many requests. Please try again later.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean shouldRateLimit(String path) {
        return path.startsWith("/oauth2/token") ||
                path.startsWith("/login") ||
                path.startsWith("/perform_login");
    }

    private boolean isRateLimited(String clientIp) {
        RequestCounter counter = requestCounts.computeIfAbsent(clientIp, k -> new RequestCounter());

        long now = System.currentTimeMillis();

        // Reset counter if window expired
        if (now - counter.windowStart > WINDOW_SIZE_MS) {
            counter.reset(now);
        }

        // Increment and check
        int currentCount = counter.count.incrementAndGet();

        // Clean up old entries periodically
        if (requestCounts.size() > 10000) {
            cleanupOldEntries(now);
        }

        return currentCount > MAX_REQUESTS_PER_MINUTE;
    }

    private void cleanupOldEntries(long now) {
        requestCounts.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_SIZE_MS);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RequestCounter {
        AtomicInteger count = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();

        void reset(long now) {
            count.set(0);
            windowStart = now;
        }
    }
}
