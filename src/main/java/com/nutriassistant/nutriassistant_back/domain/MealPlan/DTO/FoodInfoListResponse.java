package com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class FoodInfoListResponse {

    @JsonProperty("current_page")
    private int currentPage;

    @JsonProperty("page_size")
    private int pageSize;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("total_items")
    private long totalItems;

    private List<FoodInfoItem> items;

    @Getter
    @Builder
    public static class FoodInfoItem {
        @JsonProperty("menu_id")
        private String menuId;

        private String name;

        private String category;

        private Integer kcal;

        private List<Integer> allergens;

        @JsonProperty("updated_at")
        private Instant updatedAt;
    }
}
