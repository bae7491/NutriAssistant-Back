package com.nutriassistant.nutriassistant_back.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    /**
     * FastAPI 서버 베이스 URL
     * - 기본값: http://localhost:8000
     * - application.yml/properties에서 fastapi.base-url 로 덮어쓸 수 있음
     */
    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;

    /**
     * FastAPI 호출용 RestTemplate
     * - rootUri를 지정해두면 서비스에서 "/v1/.." 처럼 상대 경로로 호출 가능
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(fastApiBaseUrl)
                .build();
    }
}
