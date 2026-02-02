package com.nutriassistant.nutriassistant_back.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @CurrentUser 어노테이션을 처리하는 ArgumentResolver
 *
 * 현재 구현:
 * - Authorization 헤더에 JWT 토큰이 있으면 파싱 (TODO: JWT 구현 후 활성화)
 * - 없으면 X-User-Id, X-School-Id 헤더에서 값을 읽음 (테스트용)
 *
 * JWT 구현 후:
 * - Authorization: Bearer {JWT_TOKEN} 헤더에서 토큰을 파싱하여 사용자 정보 추출
 */
@Slf4j
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String SCHOOL_ID_HEADER = "X-School-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(UserContext.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // JWT 토큰이 있는 경우 (TODO: JWT 구현 후 활성화)
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            return parseJwtToken(token);
        }

        // 테스트용: 헤더에서 직접 값을 읽음
        return extractFromHeaders(request);
    }

    /**
     * JWT 토큰을 파싱하여 UserContext 생성
     * TODO: JWT 라이브러리 추가 후 실제 파싱 로직 구현
     */
    private UserContext parseJwtToken(String token) {
        // TODO: JWT 구현 시 아래 로직으로 교체
        // try {
        //     Claims claims = Jwts.parserBuilder()
        //             .setSigningKey(secretKey)
        //             .build()
        //             .parseClaimsJws(token)
        //             .getBody();
        //
        //     Long userId = claims.get("userId", Long.class);
        //     Long schoolId = claims.get("schoolId", Long.class);
        //     String role = claims.get("role", String.class);
        //
        //     return UserContext.of(userId, schoolId, role);
        // } catch (Exception e) {
        //     log.warn("JWT 파싱 실패: {}", e.getMessage());
        //     throw new UnauthorizedException("유효하지 않은 토큰입니다.");
        // }

        log.warn("JWT 파싱 미구현 - 헤더 기반 인증으로 대체");
        return UserContext.guest();
    }

    /**
     * 테스트용: HTTP 헤더에서 사용자 정보 추출
     * Postman 등에서 X-User-Id, X-School-Id 헤더로 테스트 가능
     */
    private UserContext extractFromHeaders(HttpServletRequest request) {
        String userIdStr = request.getHeader(USER_ID_HEADER);
        String schoolIdStr = request.getHeader(SCHOOL_ID_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        Long userId = parseOrDefault(userIdStr, 1L);
        Long schoolId = parseOrDefault(schoolIdStr, 1L);
        String userRole = (role != null && !role.isBlank()) ? role : "USER";

        log.debug("헤더 기반 인증: userId={}, schoolId={}, role={}", userId, schoolId, userRole);

        return UserContext.of(userId, schoolId, userRole);
    }

    private Long parseOrDefault(String value, Long defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
