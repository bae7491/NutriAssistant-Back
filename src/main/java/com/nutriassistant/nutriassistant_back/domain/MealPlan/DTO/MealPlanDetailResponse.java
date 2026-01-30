package com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MealPlanDetailResponse {

    @JsonProperty("menu_id")
    private Long menuId;

    @JsonProperty("meal_plan_id")
    private Long mealPlanId;

    @JsonProperty("school_id")
    private Long schoolId;

    private LocalDate date;

    @JsonProperty("meal_type")
    private String mealType;

    private Nutrition nutrition;

    private Integer cost;

    @JsonProperty("ai_comment")
    private String aiComment;

    @JsonProperty("menu_items")
    private MenuItems menuItems;

    @JsonProperty("allergen_summary")
    private AllergenSummary allergenSummary;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class Nutrition {
        private Integer kcal;
        private Integer carb;
        private Integer prot;
        private Integer fat;
    }

    @Getter
    @Builder
    public static class MenuItems {
        private MenuItem rice;
        private MenuItem soup;
        private MenuItem main1;
        private MenuItem main2;
        private MenuItem side;
        private MenuItem kimchi;
        private MenuItem dessert;
    }

    @Getter
    @Builder
    public static class MenuItem {
        private String id;
        private String name;
        private String display;
        private List<Integer> allergens;
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
