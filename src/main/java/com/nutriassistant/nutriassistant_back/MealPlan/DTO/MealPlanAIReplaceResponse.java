package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class MealPlanAIReplaceResponse {

    @JsonProperty("meal_plan_id")
    private Long mealPlanId;

    @JsonProperty("menu_id")
    private Long menuId;

    private LocalDate date;

    @JsonProperty("meal_type")
    private String mealType;

    private Boolean replaced;

    @JsonProperty("ai_comment")
    private String aiComment;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
