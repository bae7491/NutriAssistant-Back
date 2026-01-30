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
        private Long id;
        private Integer skipped_count;
        private Integer total_students;
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