package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

import java.time.LocalDate;
import java.util.List;

public record MealMenuResponse(
        Long id,
        LocalDate date,
        String type,
        String rice,
        String soup,
        String main1,
        String main2,
        String side,
        String kimchi,
        String dessert,
        List<String> rawMenus,
        Integer kcal,
        Integer carb,
        Integer prot,
        Integer fat,
        Integer cost,
        String rawMenusJson
) { }