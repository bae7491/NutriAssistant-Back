package com.nutriassistant.nutriassistant_back.MealPlan.entity;

import lombok.Getter;

@Getter
public enum MealType {
    LUNCH("중식"),
    DINNER("석식");

    private final String description;

    MealType(String description) {
        this.description = description;
    }

}
