package com.nutriassistant.nutriassistant_back.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * [JWT 인증 필터]
 *
 * 역할:
 * 1. HTTP 요청 헤더에서 JWT 토큰을 추출합니다.
 * 2. JwtProvider를 통해 토큰의 유효성을 검증합니다.
 * 3. 유효한 토큰이라면, Spring Security의 Context에 인증 정보(User)를 저장합니다.
 *
 * 특징:
 * - OncePerRequestFilter를 상속받아, 하나의 요청당 한 번만 실행됨을 보장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Request Header에서 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 유효성 검사
        if (token != null && jwtProvider.validateToken(token)) {
            // 3. 토큰이 유효하면 인증 객체(Authentication)를 가져와서 SecurityContext에 저장
            Authentication authentication = jwtProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("Security Context에 '{}' 인증 정보를 저장했습니다.", authentication.getName());
        }

        // 4. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * Request Header에서 "Bearer "로 시작하는 토큰 문자열을 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 이후의 문자열만 반환
        }
        return null;
    }
}