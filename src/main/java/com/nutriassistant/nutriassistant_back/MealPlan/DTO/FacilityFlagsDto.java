package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

public record FacilityFlagsDto(
        Boolean has_oven,
        Boolean has_fryer,
        Boolean has_griddle
) {}
