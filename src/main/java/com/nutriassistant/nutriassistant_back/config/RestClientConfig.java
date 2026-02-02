package com.nutriassistant.nutriassistant_back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * REST API 클라이언트 통합 설정 클래스
 *
 * [중요] 이 설정 파일 하나로 RestClient와 RestTemplate을 모두 관리합니다.
 * 별도의 RestTemplateConfig 파일을 만들지 마세요. (중복 에러 원인)
 */
@Configuration
public class RestClientConfig {

    /**
     * FastAPI 서버 주소 설정
     * application.yml에 'fastapi.base-url'이 없으면 기본값으로 'http://localhost:8001'을 사용합니다.
     * (FastAPI 실행 포트가 8001이므로 이에 맞췄습니다)
     */
    @Value("${fastapi.base-url:http://localhost:8001}")
    private String fastApiBaseUrl;

    /**
     * [1] RestClient 설정 (Spring 6.1+ 권장)
     * 최신 방식의 HTTP 클라이언트입니다.
     */
    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // AI 분석이 오래 걸릴 수 있으므로 타임아웃을 넉넉하게 3분(180초)으로 설정
        factory.setConnectTimeout(10000);   // 연결 시도 10초 제한
        factory.setReadTimeout(180000);     // 응답 대기 180초 제한

        return RestClient.builder()
                .baseUrl(fastApiBaseUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * [2] RestTemplate 설정 (레거시 호환용)
     * ReviewAnalysisService 등에서 사용하는 전통적인 HTTP 클라이언트입니다.
     * 이 Bean이 등록되어 있어야 'private final RestTemplate restTemplate;' 주입이 가능합니다.
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // FastAPI 분석 요청 시 타임아웃 방지를 위한 설정
        factory.setConnectTimeout(10000);   // 연결: 10초
        factory.setReadTimeout(180000);     // 읽기: 3분 (분석이 1분 이상 걸려도 끊기지 않도록 함)

        return new RestTemplate(factory);
    }
}