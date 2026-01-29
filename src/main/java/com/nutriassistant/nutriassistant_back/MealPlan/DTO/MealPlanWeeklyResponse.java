package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MealPlanWeeklyResponse {

    @JsonProperty("school_id")
    private Long schoolId;

    @JsonProperty("week_start")
    private LocalDate weekStart;

    @JsonProperty("week_end")
    private LocalDate weekEnd;

    // 네비게이션용 - 프론트에서 이전/다음 주 버튼에 사용
    @JsonProperty("prev_week_start")
    private LocalDate prevWeekStart;

    @JsonProperty("next_week_start")
    private LocalDate nextWeekStart;

    @JsonProperty("current_offset")
    private Integer currentOffset;

    private List<WeeklyMenu> menus;

    @Getter
    @Builder
    public static class WeeklyMenu {
        private Long id;
        private LocalDate date;

        @JsonProperty("meal_type")
        private String mealType;

        @JsonProperty("raw_menus")
        private List<String> rawMenus;

        @JsonProperty("allergen_summary")
        private AllergenSummary allergenSummary;
    }

    @Getter
    @Builder
    public static class AllergenSummary {
        @JsonProperty("unique_allergens")
        private List<Integer> uniqueAllergens;

        @JsonProperty("by_menu")
        private Map<String, List<Integer>> byMenu;
    }
}
