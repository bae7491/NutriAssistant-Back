package com.nutriassistant.nutriassistant_back.domain.metrics.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

public class SkipMealDto {

    @Getter
    public static class RegisterRequest {
        private Long school_id;
        private LocalDate date;
        private String meal_type;
        private Integer skipped_count;
        private Integer total_students;
    }

    @Getter
    public static class UpdateRequest {
        private Long school_id;     // 필수 (누구 학교인지)
        private LocalDate date;     // 필수 (언제 데이터인지)
        private String meal_type;   // 필수 (점심인지 저녁인지)

        private Integer skipped_count; // 수정할 값
        private Integer total_students; // 수정할 값
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long school_id;
        private LocalDate date;
        private String meal_type;
        private Integer skipped_count;
        private Integer total_students;
        private Double skip_rate;
    }

    @Getter
    @Builder
    public static class PeriodResponse {
        private Period period;
        private Long school_id;
        private String meal_type;
        private Double average_skip_rate;
        private List<Response> daily_data;
    }

    @Getter
    @Builder
    public static class Period {
        private LocalDate start_date;
        private LocalDate end_date;
    }
}