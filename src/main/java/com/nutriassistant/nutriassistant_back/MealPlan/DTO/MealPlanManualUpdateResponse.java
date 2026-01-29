package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MealPlanManualUpdateResponse {

    @JsonProperty("menu_id")
    private Long menuId;

    @JsonProperty("meal_plan_id")
    private Long mealPlanId;

    private LocalDate date;

    @JsonProperty("meal_type")
    private String mealType;

    private String reason;

    @JsonProperty("raw_menus")
    private List<String> rawMenus;

    @JsonProperty("allergen_summary")
    private AllergenSummary allergenSummary;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class AllergenSummary {
        @JsonProperty("unique_allergens")
        private List<Integer> uniqueAllergens;

        @JsonProperty("by_menu")
        private Map<String, List<Integer>> byMenu;
    }
}
