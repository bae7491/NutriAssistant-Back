package com.nutriassistant.nutriassistant_back.config;

import com.nutriassistant.nutriassistant_back.global.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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
                        // 1. [전체 공개] 로그인 안 해도 됨
                        // ====================================================
                        .requestMatchers(
                                "/api/auth/**",            // 로그인, 회원가입
                                "/api/public/**",          // 공용 API
                                "/api/schools/search/**",  // 학교 검색
                                "/internal/**",            // AI 서버 연동용
                                "/api/costs/internal/**"   // 단가 데이터 연동용 (URL 확인 필요)
                        ).permitAll()

                        // ====================================================
                        // 2. [영양사 전용] 관리자 기능 (생성, 수정, 삭제, 분석)
                        // ====================================================
                        .requestMatchers(
                                "/api/dietitian/**",       // 영양사 내 정보
                                "/api/schools/register",   // 학교 등록
                                "/api/schools/my",         // 학교 정보 수정

                                // 식단 데이터 관리 (POST, PUT, DELETE, PATCH)
                                "/mealplan/generate",      // AI 식단 생성
                                "/mealplan/confirm",       // 식단 확정
                                "/api/food/**",            // 음식 DB 관리
                                "/api/menus/cost/**",      // 단가 관리
                                "/api/reports/**",         // 운영 일지/리포트
                                "/api/metrics/**",         // 잔반/결식 데이터 등록
                                "/api/reviews/analysis/**" // 리뷰 분석 결과
                        ).hasRole("DIETITIAN")

                        // (중요) 식단표 수정/삭제는 영양사만 가능하도록 메서드로 구분
                        .requestMatchers(HttpMethod.POST, "/mealplan/**").hasRole("DIETITIAN")
                        .requestMatchers(HttpMethod.PUT, "/mealplan/**").hasRole("DIETITIAN")
                        .requestMatchers(HttpMethod.PATCH, "/mealplan/**").hasRole("DIETITIAN")

                        // ====================================================
                        // 3. [학생 전용] 사용자 기능
                        // ====================================================
                        .requestMatchers(
                                "/api/student/**"          // 학생 내 정보
                        ).hasRole("STUDENT")

                        // ====================================================
                        // 4. [공통 권한] 로그인한 누구나 (조회 기능)
                        // ====================================================
                        .requestMatchers(
                                "/mealplan/**",            // 식단 조회 (위에서 POST등을 막았으므로 여기는 GET만 남음)
                                "/api/menus/**",           // 메뉴 상세 조회
                                "/api/board/**",           // 게시판 조회 (작성 권한은 로직에 따라 분리 가능)
                                "/api/reviews/**"          // 리뷰 조회
                        ).authenticated()

                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173")); // 프론트 주소
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}