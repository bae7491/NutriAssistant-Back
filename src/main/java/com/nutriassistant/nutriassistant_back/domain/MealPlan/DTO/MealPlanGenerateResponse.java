package com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MealPlanGenerateResponse {

    private Long id;
    private LocalDate date;

    @JsonProperty("meal_type")
    private String mealType;

    private BigDecimal kcal;
    private BigDecimal carb;
    private BigDecimal prot;
    private BigDecimal fat;
    private Integer cost;

    @JsonProperty("ai_comment")
    private String aiComment;

    @JsonProperty("menu_items")
    private MenuItems menuItems;

    @JsonProperty("allergen_summary")
    private AllergenSummary allergenSummary;

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
        @JsonProperty("menu_id")
        private Long menuId;
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