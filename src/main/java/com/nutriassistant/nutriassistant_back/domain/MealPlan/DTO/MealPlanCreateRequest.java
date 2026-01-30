package com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/**
 * 식단표 생성/저장 요청 DTO
 * FastAPI 응답 데이터를 DB에 저장할 때 사용
 */
public record MealPlanCreateRequest(
        @JsonProperty("school_id")
        Long schoolId,

        @JsonProperty("year")
        Integer year,

        @JsonProperty("month")
        Integer month,

        @JsonProperty("generated_at")
        LocalDateTime generatedAt,

        /**
         * FastAPI는 "meals" 배열로 반환
         * JsonNode로 받아서 MealPlanMenuService에서 파싱
         */
        @JsonProperty("meals")
        JsonNode menus
) {}