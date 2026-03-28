package com.trafficlight.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * API Key 기반 관리자 인증 필터.
 * X-Admin-Api-Key 헤더로 관리 엔드포인트 접근을 제어한다.
 *
 * - constant-time comparison으로 타이밍 공격 방어
 * - 인증 실패 시 SecurityContext를 비워 Spring Security가 403 처리
 */
public class AdminApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Admin-Api-Key";
    private final byte[] expectedKeyBytes;

    public AdminApiKeyFilter(String expectedApiKey) {
        this.expectedKeyBytes = expectedApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null && constantTimeEquals(apiKey)) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 타이밍 공격 방어를 위한 constant-time 비교.
     * 문자열 길이나 내용에 관계없이 항상 동일한 시간이 소요된다.
     */
    private boolean constantTimeEquals(String provided) {
        byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedKeyBytes, providedBytes);
    }
}
