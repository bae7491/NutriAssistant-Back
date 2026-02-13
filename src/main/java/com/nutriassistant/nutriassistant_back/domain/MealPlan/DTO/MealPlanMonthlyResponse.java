package com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MealPlanMonthlyResponse {

    @JsonProperty("meal_plan_id")
    private Long mealPlanId;

    private Integer year;
    private Integer month;

    @JsonProperty("school_id")
    private Long schoolId;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private List<MenuDetail> menus;

    @Getter
    @Builder
    public static class MenuDetail {
        @JsonProperty("menu_id")
        private Long menuId;

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

        @JsonProperty("action_type")
        private String actionType;
    }

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
        @JsonProperty("unique")
        private List<Integer> uniqueAllergens;

        @JsonProperty("has_allergen_5")
        private Boolean hasAllergen5;
    }
}
