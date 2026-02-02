package com.nutriassistant.nutriassistant_back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정 클래스
 *
 * 역할:
 * - 애플리케이션의 보안 정책을 정의합니다.
 * - 인증(Authentication)과 인가(Authorization) 규칙을 설정합니다.
 * - CSRF, CORS 등 보안 관련 설정을 관리합니다.
 *
 * 현재 설정:
 * - CSRF 보호 비활성화 (REST API 서버이므로)
 * - 모든 요청 허용 (개발 단계, JWT 구현 후 수정 필요)
 *
 * TODO: JWT 인증 구현 시 수정 필요
 * - JwtAuthenticationFilter 추가
 * - 인증이 필요한 엔드포인트 설정
 * - 예: .requestMatchers("/boards/**").authenticated()
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Security Filter Chain 설정
     *
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     *
     * 설정 내용:
     * 1. csrf().disable() - REST API는 세션을 사용하지 않으므로 CSRF 보호 불필요
     * 2. authorizeHttpRequests() - URL별 접근 권한 설정
     *    - /internal/** : 내부 API (서버 간 통신용)
     *    - /mealplan/** : 급식 관련 API
     *    - anyRequest().permitAll() : 현재 모든 요청 허용 (개발용)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 보호 비활성화 (REST API 서버는 stateless하므로 불필요)
            .csrf(AbstractHttpConfigurer::disable)

            // URL별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 내부 서버 간 통신용 API - 인증 없이 허용
                .requestMatchers("/internal/**").permitAll()
                // 급식 관련 API - 인증 없이 허용
                .requestMatchers("/mealplan/**").permitAll()
                // 그 외 모든 요청 - 현재는 모두 허용 (TODO: JWT 구현 후 수정)
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
