package com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class MealPlanGenerateRequest {

    @NotBlank(message = "연도는 필수입니다.")
    @Pattern(regexp = "^\\d{4}$", message = "연도는 4자리 숫자여야 합니다.")
    private String year;

    @NotBlank(message = "월은 필수입니다.")
    @Pattern(regexp = "^(0?[1-9]|1[0-2])$", message = "월은 1-12 사이여야 합니다.")
    private String month;

    @NotNull(message = "옵션은 필수입니다.")
    private GenerateOptions options;

    @Getter
    @Setter
    public static class GenerateOptions {

        @JsonProperty("num_generations")
        @Min(value = 1, message = "생성 횟수는 최소 1이어야 합니다.")
        @Max(value = 31, message = "생성 횟수는 최대 31이어야 합니다.")
        private Integer numGenerations;

        @NotNull(message = "제약조건은 필수입니다.")
        private Constraints constraints;
    }

    @Getter
    @Setter
    public static class Constraints {
        @JsonProperty("nutrition_key")
        private String nutritionKey;

        @JsonProperty("target_price")
        @Positive(message = "목표 단가는 양수여야 합니다.")
        private Integer targetPrice;

        @JsonProperty("max_price_limit")
        @Positive(message = "상한 단가는 양수여야 합니다.")
        private Integer maxPriceLimit;

        @JsonProperty("cook_staff")
        @Positive(message = "조리 인력은 양수여야 합니다.")
        private Integer cookStaff;

        @JsonProperty("facility_text")
        private String facilityText;
    }
}