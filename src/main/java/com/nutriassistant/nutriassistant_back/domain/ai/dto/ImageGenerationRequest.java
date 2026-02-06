package com.nutriassistant.nutriassistant_back.domain.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ImageGenerationRequest {

    // 1. 직접 프롬프트를 입력할 경우
    private String prompt;

    // 2. 메뉴 리스트로 요청할 경우 (프롬프트 대신 사용 가능)
    private List<String> menus;

    // 유효성 검사 로직 (둘 중 하나는 있어야 함)
    public boolean isValid() {
        return (prompt != null && !prompt.isBlank()) || (menus != null && !menus.isEmpty());
    }
}