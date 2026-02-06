package com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanDetailResponse {

    @JsonProperty("menu_id")
    private Long menuId;

    @JsonProperty("meal_plan_id")
    private Long mealPlanId;

    @JsonProperty("school_id")
    private Long schoolId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonProperty("meal_type")
    private String mealType;

    // ▼▼▼ [추가] 이미지 URL (AI 생성 이미지) ▼▼▼
    @JsonProperty("image_url")
    private String imageUrl;

    // ▼▼▼ [추가] 리뷰 작성 여부 (중복 방지 UI용) ▼▼▼
    @JsonProperty("is_reviewed")
    private boolean isReviewed;

    // 영양 정보 (Map 대신 객체 사용)
    private Nutrition nutrition;

    private Integer cost;

    @JsonProperty("ai_comment")
    private String aiComment;

    // 메뉴 구성 (Map 대신 객체 사용)
    @JsonProperty("menu_items")
    private MenuItems menuItems;

    // 알레르기 요약
    @JsonProperty("allergen_summary")
    private AllergenSummary allergenSummary;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // =================================================================
    // 내부 클래스 정의 (Service 로직과 호환됨)
    // =================================================================

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Nutrition {
        private Integer kcal;
        private Integer carb;
        private Integer prot;
        private Integer fat;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuItem {
        private String id;
        private String name;
        private String display;
        private List<Integer> allergens;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllergenSummary {
        @JsonProperty("unique_allergens")
        private List<Integer> uniqueAllergens;

        @JsonProperty("by_menu")
        private Map<String, List<Integer>> byMenu;
    }
}