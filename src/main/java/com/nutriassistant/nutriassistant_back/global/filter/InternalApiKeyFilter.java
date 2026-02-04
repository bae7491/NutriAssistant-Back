package com.nutriassistant.nutriassistant_back.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-API-Key";

    @Value("${fastapi.internal-token:}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // 내부 API 경로인지 확인
        if (isInternalApiPath(requestUri)) {
            String providedKey = request.getHeader(INTERNAL_API_KEY_HEADER);

            // 키가 설정되어 있고, 제공된 키와 일치하는지 확인
            if (internalApiKey != null && !internalApiKey.isEmpty()) {
                log.info("Internal API 요청 - URI: {}, 제공된 키 존재: {}, 키 길이: {}",
                        requestUri,
                        providedKey != null,
                        providedKey != null ? providedKey.length() : 0);

                if (providedKey == null || !providedKey.equals(internalApiKey)) {
                    log.warn("Internal API 접근 거부: 잘못된 API 키 - URI: {}, 설정된 키 길이: {}, 제공된 키 길이: {}",
                            requestUri,
                            internalApiKey.length(),
                            providedKey != null ? providedKey.length() : 0);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\": \"Invalid or missing internal API key\"}");
                    return;
                }
                log.info("Internal API 접근 허용: {}", requestUri);
            } else {
                // 키가 설정되지 않은 경우 경고 로그 (개발 환경)
                log.warn("Internal API Key가 설정되지 않았습니다. 보안을 위해 설정을 권장합니다.");
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isInternalApiPath(String uri) {
        return uri.startsWith("/internal/") || uri.startsWith("/api/costs/internal/");
    }
}
