package com.nutriassistant.nutriassistant_back.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {

    // GCP 프로젝트 ID
    private static final String PROJECT_ID = "food-r-419209";
    // 모델 ID (Imagen 3.0 최신 모델 사용 권장)
    private static final String MODEL_ID = "imagen-3.0-generate-001";
    private static final String LOCATION = "us-central1";

    private static final String API_URL = String.format(
            "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict",
            LOCATION, PROJECT_ID, LOCATION, MODEL_ID
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 식단 메뉴 리스트를 받아 AI 이미지 생성 (편의 메서드)
     */
    public String generateMealPlanImage(List<String> menuNames) throws IOException {
        String menuString = String.join(", ", menuNames);
        // 프롬프트 튜닝
        String prompt = "A high-quality, top-down food photography of a Korean school lunch tray containing: "
                + menuString + ". Realistic, appetizing, bright lighting.";
        return generateImage(prompt);
    }

    /**
     * 프롬프트 문자열을 받아 AI 이미지 생성 (핵심 로직)
     * @return Base64 Encoded Image String
     */
    public String generateImage(String prompt) throws IOException {
        // 1. 인증 토큰 발급 (resources 파일 로드 방식)
        ClassPathResource resource = new ClassPathResource("food-r-419209-0c382bb35fd3.json");

        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream())
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

        credentials.refreshIfExpired();
        AccessToken token = credentials.getAccessToken();

        // 2. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getTokenValue());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 3. 요청 바디 구성
        Map<String, Object> instance = new HashMap<>();
        instance.put("prompt", prompt);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("sampleCount", 1);
        parameters.put("aspectRatio", "1:1");

        // [삭제됨] parameters.put("storageUri", "");
        // -> 이 줄을 지웠으므로 이제 에러 없이 Base64 코드로 응답이 옵니다.

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("instances", List.of(instance));
        requestBody.put("parameters", parameters);

        // 4. API 호출
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);

        // 5. 응답 파싱
        return extractBase64FromJson(response.getBody());
    }

    /**
     * Vertex AI 응답 JSON에서 실제 이미지 데이터(Base64)만 추출하는 메서드
     */
    private String extractBase64FromJson(String jsonResponse) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode predictions = rootNode.path("predictions");
        if (predictions.isArray() && !predictions.isEmpty()) {
            return predictions.get(0).path("bytesBase64Encoded").asText();
        }
        throw new IOException("이미지 생성 실패: 유효한 응답을 받지 못했습니다.");
    }
}