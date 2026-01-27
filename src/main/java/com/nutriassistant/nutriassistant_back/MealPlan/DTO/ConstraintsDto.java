package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

public record ConstraintsDto(
        FacilityFlagsDto facility_flags,
        String facility_text,
        Integer target_price,
        Double cost_tolerance,
        Integer max_price_limit,
        Integer cook_staff
) {}
