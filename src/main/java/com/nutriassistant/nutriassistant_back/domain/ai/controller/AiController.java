package com.nutriassistant.nutriassistant_back.domain.ai.controller;

import com.nutriassistant.nutriassistant_back.domain.ai.dto.ImageGenerationRequest;
import com.nutriassistant.nutriassistant_back.domain.ai.service.ImageGenerationService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // [추가] 로그 기록용
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Slf4j // [추가] 심볼 'log' 해결
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final ImageGenerationService imageGenerationService;

    /**
     * AI 이미지 생성 및 S3 저장 API
     * 프롬프트 또는 메뉴 리스트를 받아 생성된 이미지의 S3 URL을 반환합니다.
     */
    @PostMapping("/image/generate")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateImage(@RequestBody ImageGenerationRequest request) {

        // 1. 유효성 검사
        if (!request.isValid()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("프롬프트 또는 메뉴 리스트 중 하나는 필수입니다."));
        }

        try {
            String imageUrl;

            // 2. 메뉴 리스트 또는 프롬프트를 사용하여 이미지 생성 및 S3 업로드 실행
            if (request.getMenus() != null && !request.getMenus().isEmpty()) {
                log.info("🍱 메뉴 리스트 기반 이미지 생성 시작: {}", request.getMenus());
                imageUrl = imageGenerationService.generateAndSaveMealImage(request.getMenus());
            } else {
                log.info("📝 프롬프트 기반 이미지 생성 시작: {}", request.getPrompt());
                // 기존 generateImage 로직을 S3 업로드와 결합하거나
                // 서비스에서 직접 처리하도록 설계된 메서드를 호출합니다.
                String base64 = imageGenerationService.generateImage(request.getPrompt());
                // ※ 참고: 프롬프트 직접 입력 시에도 S3 저장이 필요하다면 서비스의 uploadBase64ToS3를 활용하세요.
                imageUrl = "data:image/png;base64," + base64;
            }

            // 3. S3 URL(또는 Base64) 반환
            return ResponseEntity.ok()
                    .body(ApiResponse.success(Collections.singletonMap("imageUrl", imageUrl)));

        } catch (IOException e) {
            log.error("❌ 이미지 생성 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("이미지 생성 및 저장 중 오류가 발생했습니다."));
        }
    }
}