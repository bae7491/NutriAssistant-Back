package com.nutriassistant.nutriassistant_back.domain.metrics.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

public class LeftoverDto {

    @Getter
    public static class RegisterRequest {
        // school_id는 JWT에서 자동 추출
        private LocalDate date;
        private String meal_type;
        private Float amount_kg;
    }

    @Getter
    public static class UpdateRequest {
        // school_id는 JWT에서 자동 추출
        private LocalDate date;
        private String meal_type;

        private Double amount_kg; // 수정할 값
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long school_id;
        private LocalDate date;
        private String meal_type;
        private Float amount_kg;
    }

    @Getter
    @Builder
    public static class PeriodResponse {
        private Period period;
        private Long school_id;
        private String meal_type;
        private Double average_amount_kg;
        private List<Response> daily_data;
    }

    @Getter
    @Builder
    public static class Period {
        private LocalDate start_date;
        private LocalDate end_date;
    }
}