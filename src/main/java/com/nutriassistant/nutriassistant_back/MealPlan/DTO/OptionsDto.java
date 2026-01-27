package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

public record OptionsDto(
        ConstraintsDto constraints,
        int numGenerations
) {}
