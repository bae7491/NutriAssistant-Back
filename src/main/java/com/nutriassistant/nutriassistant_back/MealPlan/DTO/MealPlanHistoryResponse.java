package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MealPlanHistoryResponse {

    private Integer count;
    private List<HistoryItem> items;

    @Getter
    @Builder
    public static class HistoryItem {
        private Long id;

        @JsonProperty("mealDate")
        private String mealDate;

        @JsonProperty("mealType")
        private String mealType;

        @JsonProperty("actionType")
        private String actionType;

        @JsonProperty("oldMenus")
        private List<String> oldMenus;

        @JsonProperty("newMenus")
        private List<String> newMenus;

        private String reason;
    }
}
