package com.nutriassistant.nutriassistant_back.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {

    private static final String PROJECT_ID = "food-r-419209";
    private static final String MODEL_ID = "imagen-3.0-generate-001";
    private static final String LOCATION = "us-central1";
    private static final String API_URL = String.format(
            "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict",
            LOCATION, PROJECT_ID, LOCATION, MODEL_ID
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /* [v2 적용] S3Config에서 등록한 S3Client Bean을 주입받습니다. */
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    /*
     * 메뉴 목록을 받아 AI 이미지를 생성하고 S3에 저장한 후 URL을 반환하는 통합 메서드입니다.
     */
    public String generateAndSaveMealImage(List<String> menuNames) throws IOException {
        String menuString = String.join(", ", menuNames);
        String prompt = "A high-quality, top-down food photography of a Korean school lunch tray containing: "
                + menuString + ". Realistic, appetizing, bright lighting.";

        /* 1. GCP API를 통해 Base64 이미지 데이터를 생성합니다. */
        String base64Image = generateImage(prompt);

        /* 2. 생성된 데이터를 S3에 업로드하고 저장된 URL을 획득합니다. */
        return uploadBase64ToS3(base64Image);
    }

    /*
     * GCP Vertex AI를 호출하여 프롬프트에 따른 이미지를 생성합니다.
     */
    public String generateImage(String prompt) throws IOException {
        ClassPathResource resource = new ClassPathResource("food-r-419209-0c382bb35fd3.json");
        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream())
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

        credentials.refreshIfExpired();
        AccessToken token = credentials.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getTokenValue());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> instance = new HashMap<>();
        instance.put("prompt", prompt);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("sampleCount", 1);
        parameters.put("aspectRatio", "1:1");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("instances", List.of(instance));
        requestBody.put("parameters", parameters);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);

        return extractBase64FromJson(response.getBody());
    }

    /*
     * Base64 데이터를 디코딩하여 AWS SDK v2 방식으로 S3에 업로드합니다.
     */
    private String uploadBase64ToS3(String base64Image) throws IOException {
        /* 1. Base64 인코딩된 문자열을 바이트 배열로 변환합니다. */
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        String fileName = "meal-plans/" + UUID.randomUUID() + ".png";

        try {
            /* 2. v2 방식의 PutObjectRequest를 빌더를 통해 생성합니다. */
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType("image/png")
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            /* 3. RequestBody.fromBytes를 사용하여 바이너리 데이터를 직접 전송합니다. */
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));

            /* 4. 업로드된 파일의 Public URL을 형식에 맞춰 생성합니다. */
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, fileName);
        } catch (Exception e) {
            log.error("S3 업로드 에러 발생: {}", e.getMessage());
            throw new IOException("S3 이미지 저장에 실패하였습니다.");
        }
    }

    /*
     * JSON 응답 본문에서 이미지 데이터(Base64)를 파싱합니다.
     */
    private String extractBase64FromJson(String jsonResponse) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode predictions = rootNode.path("predictions");
        if (predictions.isArray() && !predictions.isEmpty()) {
            return predictions.get(0).path("bytesBase64Encoded").asText();
        }
        throw new IOException("유효한 이미지 응답을 받지 못했습니다.");
    }
}