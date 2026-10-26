package com.healthrecords.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight, in-memory rate limiting.
 * Two buckets: a strict one for /api/auth/** (credential stuffing) and a
 * looser one for sensitive writes (record/prescription/lab-report/MFA
 * writes and audit-log reads). This is a single-instance guard, not a
 * distributed rate limiter: in a multi-instance deployment, enforce limits
 * at the API gateway / load balancer (or with Redis) instead.
 * Normal frontend use is far below both defaults.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxRequestsPerMinute;
    private final int sensitiveMaxRequestsPerMinute;
    private final Map<String, Deque<Long>> requestsByIp = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> sensitiveRequestsByIp = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.auth.max-requests-per-minute:30}") int maxRequestsPerMinute,
                           @Value("${app.rate-limit.sensitive.max-requests-per-minute:120}") int sensitiveMaxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.sensitiveMaxRequestsPerMinute = sensitiveMaxRequestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/")) {
            return false;
        }
        String method = request.getMethod();
        boolean isWrite = "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
        return !(isWrite && (uri.startsWith("/api/medical-records") || uri.startsWith("/api/prescriptions")
                || uri.startsWith("/api/lab-reports") || uri.startsWith("/api/auth/mfa"))
                || uri.startsWith("/api/audit-logs"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean isAuth = uri.startsWith("/api/auth/");
        Map<String, Deque<Long>> bucket = isAuth ? requestsByIp : sensitiveRequestsByIp;
        int limit = isAuth ? maxRequestsPerMinute : sensitiveMaxRequestsPerMinute;
        String clientIp = clientIp(request);
        long now = Instant.now().toEpochMilli();
        long windowStart = now - 60_000L;
        Deque<Long> timestamps = bucket.computeIfAbsent(clientIp, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":429,\"error\":\"Too many requests. Please wait a minute and try again.\"}");
                return;
            }
            timestamps.addLast(now);
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
