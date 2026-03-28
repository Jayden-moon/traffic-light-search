package com.trafficlight.api.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * A01: Broken Access Control / A07: Authentication Failures
 * 관리 API 엔드포인트에 API Key 인증을 적용한다.
 * - 검색/조회 엔드포인트: 공개
 * - 인덱스 관리/동기화: 인증 필요
 * - 시작 시 admin-key 기본값 사용 경고
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String DEFAULT_KEY = "dev-admin-key-change-in-production";

    @Value("${trafficlight.security.admin-key:" + DEFAULT_KEY + "}")
    private String adminApiKey;

    @PostConstruct
    void validateAdminKey() {
        if (DEFAULT_KEY.equals(adminApiKey)) {
            log.warn("============================================================");
            log.warn("  WARNING: Admin API Key is using the default value!");
            log.warn("  Set ADMIN_API_KEY environment variable for production.");
            log.warn("============================================================");
        }
        if (adminApiKey == null || adminApiKey.isBlank()) {
            throw new IllegalStateException("Admin API Key must not be empty. Set ADMIN_API_KEY environment variable.");
        }
        if (adminApiKey.length() < 16) {
            log.warn("Admin API Key is shorter than 16 characters. Consider using a stronger key.");
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new AdminApiKeyFilter(adminApiKey), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) -> res.sendError(403, "Forbidden"))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/aggregations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/index/status").permitAll()
                .requestMatchers("/api/index/**").hasRole("ADMIN")
                .requestMatchers("/api/sync/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
