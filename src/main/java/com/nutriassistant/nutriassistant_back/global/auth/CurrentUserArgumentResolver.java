package com.nutriassistant.nutriassistant_back.global.auth;

import com.nutriassistant.nutriassistant_back.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
 * JWT 토큰에서 사용자 정보(userId, schoolId, role)를 추출하여 UserContext 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String SCHOOL_ID_HEADER = "X-School-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final JwtProvider jwtProvider;

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

        // JWT 토큰이 있는 경우
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            return parseJwtToken(token);
        }

        // 테스트용: 헤더에서 직접 값을 읽음
        return extractFromHeaders(request);
    }

    /**
     * JWT 토큰을 파싱하여 UserContext 생성
     */
    private UserContext parseJwtToken(String token) {
        try {
            if (!jwtProvider.validateToken(token)) {
                log.warn("JWT 토큰 검증 실패");
                return UserContext.guest();
            }

            Long userId = jwtProvider.getUserId(token);
            Long schoolId = jwtProvider.getSchoolId(token);
            String role = jwtProvider.getRole(token);

            log.debug("JWT 인증 성공: userId={}, schoolId={}, role={}", userId, schoolId, role);
            return UserContext.of(userId, schoolId, role);
        } catch (Exception e) {
            log.warn("JWT 파싱 실패: {}", e.getMessage());
            return UserContext.guest();
        }
    }

    /**
     * 테스트용: HTTP 헤더에서 사용자 정보 추출
     * Postman 등에서 X-User-Id, X-School-Id, X-User-Role 헤더로 테스트 가능
     *
     * 예시:
     * - X-User-Id: 1 (학생 또는 영양사 ID)
     * - X-School-Id: 1 (학교 ID)
     * - X-User-Role: ROLE_STUDENT 또는 ROLE_DIETITIAN
     */
    private UserContext extractFromHeaders(HttpServletRequest request) {
        String userIdStr = request.getHeader(USER_ID_HEADER);
        String schoolIdStr = request.getHeader(SCHOOL_ID_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        // 테스트용: 기본값 설정 (헤더가 없을 경우)
        Long userId = parseOrDefault(userIdStr, 1L);
        Long schoolId = parseOrDefault(schoolIdStr, 1L);
        String userRole = (role != null && !role.isBlank()) ? role : "ROLE_DIETITIAN";

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
