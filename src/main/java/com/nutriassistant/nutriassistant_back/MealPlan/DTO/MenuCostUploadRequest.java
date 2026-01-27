package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

import java.util.Map;

public record MenuCostUploadRequest(
        Integer year,
        Map<String, Integer> prices
) {
}