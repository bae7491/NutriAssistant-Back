package com.nutriassistant.nutriassistant_back.domain.MealPlan.entity;

import lombok.Getter;

@Getter
public enum NutritionStandard {
    ELEMENTARY("초등", null, "초등학교"),
    MIDDLE_MALE("중등", "남", "남자중학교"),
    MIDDLE_FEMALE("중등", "여", "여자중학교"),
    MIDDLE_COED("중등", "남녀", "남녀공학 중학교"),
    HIGH_MALE("고등", "남", "남자고등학교"),
    HIGH_FEMALE("고등", "여", "여자고등학교"),
    HIGH_COED("고등", "남녀", "남녀공학 고등학교");

    private final String level;
    private final String gender;
    private final String description;

    NutritionStandard(String level, String gender, String description) {
        this.level = level;
        this.gender = gender;
        this.description = description;
    }
}
