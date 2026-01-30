package com.nutriassistant.nutriassistant_back.domain.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReviewDto {

    // [요청] 리뷰 등록
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        private Long student_id;
        private Long school_id;
        private LocalDate date;
        private String meal_type;
        private Integer rating;
        private String content;
    }

    // [응답] 등록 결과 및 조회
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long student_id;
        private Long school_id;
        private LocalDate date;
        private String meal_type;
        private Integer rating;
        private String content;
        private LocalDateTime created_at;
    }
}