package com.nutriassistant.nutriassistant_back.domain.ai.controller;

import com.nutriassistant.nutriassistant_back.domain.ai.dto.ImageGenerationRequest;
import com.nutriassistant.nutriassistant_back.domain.ai.service.ImageGenerationService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final ImageGenerationService imageGenerationService;

    /**
     * AI 이미지 생성 API
     * 프롬프트 문자열 또는 메뉴 리스트를 받아 이미지를 생성합니다.
     */
    @PostMapping("/image/generate")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateImage(@RequestBody ImageGenerationRequest request) {

        // 유효성 검사
        if (!request.isValid()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("프롬프트 또는 메뉴 리스트 중 하나는 필수입니다."));
        }

        try {
            String base64Image;

            // 메뉴 리스트가 있으면 우선 사용
            if (request.getMenus() != null && !request.getMenus().isEmpty()) {
                base64Image = imageGenerationService.generateMealPlanImage(request.getMenus());
            } else {
                base64Image = imageGenerationService.generateImage(request.getPrompt());
            }

            // 프론트엔드에서 바로 사용할 수 있도록 포맷팅 (data:image/png;base64,...)
            String result = "data:image/png;base64," + base64Image;

            // JSON 형태로 반환
            return ResponseEntity.ok()
                    .body(ApiResponse.success(Collections.singletonMap("imageUrl", result)));

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("이미지 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}