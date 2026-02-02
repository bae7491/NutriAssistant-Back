package com.nutriassistant.nutriassistant_back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * REST API 클라이언트 설정 클래스
 *
 * 역할:
 * - 외부 API 호출을 위한 HTTP 클라이언트를 설정합니다.
 * - FastAPI 서버(AI 분석 서버)와의 통신에 사용됩니다.
 * - 타임아웃 등 연결 설정을 관리합니다.
 *
 * 제공하는 Bean:
 * 1. RestClient - Spring 6.1+의 새로운 HTTP 클라이언트 (권장)
 * 2. RestTemplate - 레거시 HTTP 클라이언트 (하위 호환용)
 *
 * 설정값:
 * - fastapi.base-url: FastAPI 서버 주소 (application.yml에서 설정)
 * - 연결 타임아웃: 10초 (서버 연결까지 대기 시간)
 * - 읽기 타임아웃: 180초 (AI 분석 응답 대기 시간, 3분)
 */
@Configuration
public class RestClientConfig {

    /**
     * FastAPI 서버 기본 URL
     * application.yml에서 fastapi.base-url로 설정
     * 설정이 없으면 기본값 http://localhost:8001 사용
     */
    @Value("${fastapi.base-url:http://localhost:8001}")
    private String fastApiBaseUrl;

    /**
     * RestClient Bean 설정 (Spring 6.1+ 권장)
     *
     * 특징:
     * - Fluent API 제공 (체이닝 방식으로 요청 구성)
     * - 동기/비동기 모두 지원
     * - 타입 안전한 응답 처리
     *
     * 사용 예시:
     * <pre>
     * String result = restClient.get()
     *     .uri("/api/analyze")
     *     .retrieve()
     *     .body(String.class);
     * </pre>
     */
    @Bean
    public RestClient restClient() {
        // 타임아웃 설정 (AI 분석은 시간이 오래 걸릴 수 있음)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);   // 연결 타임아웃: 10초
        factory.setReadTimeout(180000);     // 읽기 타임아웃: 180초 (3분)

        // RestClient 생성 및 Base URL 설정
        return RestClient.builder()
                .baseUrl(fastApiBaseUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * RestTemplate Bean 설정 (레거시, 하위 호환용)
     *
     * 특징:
     * - Spring 3.0부터 사용된 전통적인 HTTP 클라이언트
     * - 동기 방식으로만 동작
     * - 새 프로젝트에서는 RestClient 사용 권장
     *
     * 사용 예시:
     * <pre>
     * String result = restTemplate.getForObject("/api/analyze", String.class);
     * </pre>
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);   // 연결 타임아웃: 10초
        factory.setReadTimeout(180000);     // 읽기 타임아웃: 180초 (3분)
        return new RestTemplate(factory);
    }
}
