package com.nutriassistant.nutriassistant_back.config;

import com.nutriassistant.nutriassistant_back.global.filter.InternalApiKeyFilter;
import com.nutriassistant.nutriassistant_back.global.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          InternalApiKeyFilter internalApiKeyFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.internalApiKeyFilter = internalApiKeyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // ====================================================
                        // 1. [완전 공개] 로그인, 회원가입, 찾기 등 (토큰 없이 접근 가능)
                        // ====================================================
                        .requestMatchers(
                                "/api/auth/login/**",          // 로그인
                                "/api/auth/signup/**",         // 회원가입

                                // ▼▼▼ [수정] 아이디/비번 찾기 경로 확장 (중요!) ▼▼▼
                                "/api/auth/find-id/**",
                                "/api/auth/find-pw/**",
                                "/api/dietitian/find-id",      // 영양사 아이디 찾기 (만약 DietitianController에 있다면)
                                "/api/dietitian/find-pw",      // 영양사 비번 찾기
                                "/api/student/find-id",        // 학생 아이디 찾기 (만약 StudentController에 있다면)
                                "/api/student/find-pw",        // 학생 비번 찾기

                                "/api/dietitian/signup",       // 영양사 회원가입 (이것도 로그인 전이므로 허용해야 함)
                                "/api/student/signup",         // 학생 회원가입

                                "/api/public/**",              // 공용 API
                                "/api/schools/search/**",      // 학교 검색
                                "/internal/**",                // AI 서버 연동
                                "/api/costs/internal/**"       // 단가 데이터 연동
                        ).permitAll()

                        // ====================================================
                        // 2. [인증 필수] 회원 탈퇴, 로그아웃, 비밀번호 변경 (로그인 상태여야 함)
                        // ====================================================
                        .requestMatchers(
                                "/api/auth/withdraw/**",
                                "/api/auth/logout",
                                "/api/auth/password/**"
                        ).authenticated()

                        // ====================================================
                        // 3. [영양사 전용]
                        // ====================================================
                        .requestMatchers(
                                "/api/dietitian/**",           // 주의: find-id가 여기 포함되면 막히므로 위에서 먼저 permitAll 해줘야 함
                                "/api/schools/register",
                                "/api/schools/my",
                                "/mealplan/generate",
                                "/mealplan/confirm",
                                "/api/food/**",
                                "/api/menus/cost/**",
                                "/api/reports/**",
                                "/api/metrics/**",
                                "/api/reviews/analysis/**"
                        ).hasRole("DIETITIAN")

                        .requestMatchers(HttpMethod.POST, "/mealplan/**").hasRole("DIETITIAN")
                        .requestMatchers(HttpMethod.PUT, "/mealplan/**").hasRole("DIETITIAN")
                        .requestMatchers(HttpMethod.PATCH, "/mealplan/**").hasRole("DIETITIAN")

                        // ====================================================
                        // 4. [학생 전용]
                        // ====================================================
                        .requestMatchers(
                                "/api/student/**"              // 주의: find-id가 여기 포함되면 막힘
                        ).hasRole("STUDENT")

                        // ====================================================
                        // 5. [공통] 나머지 모든 요청은 인증 필요
                        // ====================================================
                        .requestMatchers(
                                "/mealplan/**",
                                "/api/menus/**",
                                "/api/board/**",
                                "/api/reviews/**"
                        ).authenticated()

                        .anyRequest().authenticated()
                );

        // 필터 순서 설정
        http.addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://kt-team18-student-prod-web.s3-website.ap-northeast-2.amazonaws.com",
                "http://kt-team18-school-prod-web.s3-website.ap-northeast-2.amazonaws.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}