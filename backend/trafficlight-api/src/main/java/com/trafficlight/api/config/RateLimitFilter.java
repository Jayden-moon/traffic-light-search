package com.trafficlight.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP 기반 Rate Limiting 필터.
 * - 공개 엔드포인트: IP당 분당 60회
 * - 관리 엔드포인트: IP당 분당 10회
 * - 초과 시 429 Too Many Requests 반환
 *
 * 단일 인스턴스 환경용. 분산 환경에서는 Redis 기반 Rate Limiter(Bucket4j 등)로 교체 필요.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int PUBLIC_LIMIT_PER_MINUTE = 60;
    private static final int ADMIN_LIMIT_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000;

    private final Map<String, RateWindow> publicWindows = new ConcurrentHashMap<>();
    private final Map<String, RateWindow> adminWindows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        boolean isAdminPath = path.startsWith("/api/index/") && !"GET".equals(request.getMethod())
                || path.startsWith("/api/sync");

        Map<String, RateWindow> windows = isAdminPath ? adminWindows : publicWindows;
        int limit = isAdminPath ? ADMIN_LIMIT_PER_MINUTE : PUBLIC_LIMIT_PER_MINUTE;

        RateWindow window = windows.compute(clientIp, (ip, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateWindow(now, new AtomicInteger(0));
            }
            return existing;
        });

        int count = window.counter.incrementAndGet();

        if (count > limit) {
            log.warn("Rate limit exceeded for IP: {} on path: {} ({}/{})", clientIp, path, count, limit);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record RateWindow(long windowStart, AtomicInteger counter) {}
}
