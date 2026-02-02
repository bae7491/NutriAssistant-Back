package com.nutriassistant.nutriassistant_back.config;

import com.nutriassistant.nutriassistant_back.global.auth.CurrentUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 설정 클래스
 *
 * 역할:
 * - Spring MVC의 동작을 커스터마이징합니다.
 * - 컨트롤러 메서드의 파라미터 처리 방식을 확장합니다.
 * - 인터셉터, CORS, 리소스 핸들러 등을 설정할 수 있습니다.
 *
 * 현재 설정:
 * - @CurrentUser 어노테이션을 처리하는 ArgumentResolver 등록
 *
 * ArgumentResolver란?
 * - 컨트롤러 메서드의 파라미터를 자동으로 주입해주는 컴포넌트
 * - 예: @PathVariable, @RequestBody, @RequestParam 등도 ArgumentResolver로 처리됨
 * - 커스텀 어노테이션(@CurrentUser)도 ArgumentResolver로 처리 가능
 *
 * 사용 예시:
 * <pre>
 * @GetMapping("/boards/{id}")
 * public ResponseEntity<?> getBoard(
 *     @CurrentUser UserContext user,  // ArgumentResolver가 자동 주입
 *     @PathVariable Long id
 * ) {
 *     // user.getUserId(), user.getSchoolId() 사용 가능
 * }
 * </pre>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * @CurrentUser 어노테이션을 처리하는 ArgumentResolver
     * - HTTP 헤더에서 사용자 정보 추출 (현재: X-User-Id, X-School-Id)
     * - JWT 구현 후에는 토큰에서 추출하도록 수정 예정
     */
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebMvcConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    /**
     * 커스텀 ArgumentResolver 등록
     *
     * @param resolvers ArgumentResolver 목록
     *
     * 등록된 Resolver:
     * 1. CurrentUserArgumentResolver - @CurrentUser UserContext 파라미터 처리
     *
     * 추가 가능한 Resolver 예시:
     * - PageableArgumentResolver (페이징 처리)
     * - CustomHeaderArgumentResolver (커스텀 헤더 처리)
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    /*
     * 추가 가능한 설정들:
     *
     * 1. CORS 설정
     * @Override
     * public void addCorsMappings(CorsRegistry registry) {
     *     registry.addMapping("/**")
     *         .allowedOrigins("http://localhost:3000")
     *         .allowedMethods("GET", "POST", "PUT", "DELETE");
     * }
     *
     * 2. 인터셉터 설정
     * @Override
     * public void addInterceptors(InterceptorRegistry registry) {
     *     registry.addInterceptor(new LoggingInterceptor())
     *         .addPathPatterns("/**");
     * }
     *
     * 3. 정적 리소스 핸들러
     * @Override
     * public void addResourceHandlers(ResourceHandlerRegistry registry) {
     *     registry.addResourceHandler("/static/**")
     *         .addResourceLocations("classpath:/static/");
     * }
     */
}
