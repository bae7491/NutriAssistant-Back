package com.nutriassistant.nutriassistant_back.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DayMenu(
        String date,
        MealEmbedded lunch,
        MealEmbedded dinner
) {}