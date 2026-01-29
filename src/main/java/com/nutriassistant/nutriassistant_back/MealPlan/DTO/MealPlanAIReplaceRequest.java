package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MealPlanAIReplaceRequest {

    @NotBlank(message = "날짜는 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식은 YYYY-MM-DD여야 합니다.")
    private String date;

    @NotBlank(message = "식사 유형은 필수입니다.")
    private String mealType;
}
