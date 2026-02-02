package com.nutriassistant.nutriassistant_back.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger (OpenAPI 3.0) 설정 클래스
 *
 * 역할:
 * - API 문서를 자동으로 생성합니다.
 * - Swagger UI를 통해 API를 테스트할 수 있습니다.
 *
 * 접속 URL:
 * - Swagger UI: http://localhost:8080/swagger-ui/index.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 *
 * 현재 설정:
 * - JWT Bearer 토큰 인증 헤더 지원 (Authorization)
 * - 테스트용 커스텀 헤더 지원 (X-User-Id, X-School-Id)
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // JWT Bearer 토큰 인증 스키마 (추후 JWT 구현 시 사용)
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT 토큰을 입력하세요. (Bearer 접두사 자동 추가)");

        // 테스트용 X-User-Id 헤더 스키마
        SecurityScheme userIdScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-User-Id")
                .description("테스트용 사용자 ID (기본값: 1)");

        // 테스트용 X-School-Id 헤더 스키마
        SecurityScheme schoolIdScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-School-Id")
                .description("테스트용 학교 ID (기본값: 1)");

        // Security Requirement 설정
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("JWT")
                .addList("X-User-Id")
                .addList("X-School-Id");

        return new OpenAPI()
                // API 기본 정보
                .info(new Info()
                        .title("NutriAssistant API")
                        .version("1.0.0")
                        .description("""
                                ## 급식 도우미 서비스 API

                                ### 인증 방법
                                - **JWT 토큰**: Authorization 헤더에 Bearer 토큰 입력 (추후 구현)
                                - **테스트 모드**: X-User-Id, X-School-Id 헤더로 직접 입력

                                ### 테스트 헤더 예시
                                ```
                                X-User-Id: 1
                                X-School-Id: 1
                                ```
                                """)
                        .contact(new Contact()
                                .name("NutriAssistant Team")
                                .email("support@nutriassistant.com")))

                // 서버 정보
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("로컬 개발 서버")
                ))

                // 인증 스키마 등록
                .components(new Components()
                        .addSecuritySchemes("JWT", jwtScheme)
                        .addSecuritySchemes("X-User-Id", userIdScheme)
                        .addSecuritySchemes("X-School-Id", schoolIdScheme))

                // 전역 Security 적용
                .addSecurityItem(securityRequirement);
    }
}
