package com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO;

// 단가 정보 응답
public record MenuCostResponse(
        String menuName,
        Integer price,
        Integer baseYear,
        Integer currentYear,
        Double inflationMultiplier
) {}
