package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MealPlanHistoryResponse {

    @JsonProperty("current_page")
    private Integer currentPage;

    @JsonProperty("page_size")
    private Integer pageSize;

    @JsonProperty("total_items")
    private Long totalItems;

    @JsonProperty("total_pages")
    private Integer totalPages;

    private List<HistoryItem> items;

    @Getter
    @Builder
    public static class HistoryItem {
        private Long id;

        @JsonProperty("meal_date")
        private String mealDate;

        @JsonProperty("meal_type")
        private String mealType;

        @JsonProperty("action_type")
        private String actionType;

        @JsonProperty("old_menus")
        private List<String> oldMenus;

        @JsonProperty("new_menus")
        private List<String> newMenus;

        private String reason;

        @JsonProperty("menu_created_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime menuCreatedAt;

        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }
}
