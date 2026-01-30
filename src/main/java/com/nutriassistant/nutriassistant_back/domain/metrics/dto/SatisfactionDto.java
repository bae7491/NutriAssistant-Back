package com.nutriassistant.nutriassistant_back.domain.metrics.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class SatisfactionDto {

    // --- 공통 VO ---

    @Getter
    @Builder
    public static class Period {
        private LocalDate start_date;
        private LocalDate end_date;
    }

    @Getter
    @Builder
    public static class Pagination {
        private Integer current_page;
        private Integer total_pages;
        private Long total_items;
        private Integer page_size;
    }

    // --- 1. 최근 30일 만족도 건수 조회 (GET /metrics/satisfaction/count/last-30days) ---

    @Getter
    @Builder
    public static class CountResponse {
        private Period period;
        private Long school_id;
        private Long total_count;
        private Long positive_count;
        private Long negative_count;
        private Long neutral_count;
    }

    // --- 2. 최근 30일 만족도 목록 조회 (GET /metrics/satisfaction/last-30days) ---

    @Getter
    @Builder
    public static class BatchListResponse {
        private Period period;
        private Long school_id;
        private List<BatchInfo> batches;
    }

    @Getter
    @Builder
    public static class BatchInfo {
        private String batch_id;
        private LocalDate date;
        private LocalDateTime generated_at;
        private String model_version;
        private Integer total_reviews;
        private Double average_rating;
        private Integer positive_count;
        private Integer negative_count;
    }

    // --- 3. 긍정/부정 만족도 건수 조회 (GET /metrics/satisfaction/{positive|negative}/count) ---

    @Getter
    @Builder
    public static class LabelCountResponse {
        private Long school_id;
        private String sentiment_label; // POSITIVE, NEGATIVE
        private Long count;
        private Period period;
    }

    // --- 4. 만족도 리뷰 내용 조회 (GET /metrics/satisfaction/reviews) ---

    @Getter
    @Builder
    public static class ReviewListResponse {
        private List<ReviewDetail> reviews;
    }

    @Getter
    @Builder
    public static class ReviewDetail {
        private String review_id;
        private String batch_id;
        private Long school_id;
        private String meal_type;
        private LocalDate date;
        private Double rating_5;

        private String sentiment_label;
        private Double sentiment_score;
        private Double sentiment_conf;

        private List<String> aspect_tags;
        private Map<String, String> aspect_hints;
        private Map<String, AspectDetailInfo> aspect_details;

        private List<String> evidence_phrases;
        private List<String> issue_flags;
    }

    @Getter
    @Builder
    public static class AspectDetailInfo {
        private String polarity;
        private String hint;
    }
}