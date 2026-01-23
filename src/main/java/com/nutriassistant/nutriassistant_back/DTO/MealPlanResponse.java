package com.nutriassistant.nutriassistant_back.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record MealPlanResponse(
        Long mealPlanId,
        int year,
        int month,
        LocalDateTime generatedAt,
        List<MealMenuResponse> menus
) {}