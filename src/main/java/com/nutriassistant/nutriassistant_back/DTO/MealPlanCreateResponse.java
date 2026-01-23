package com.nutriassistant.nutriassistant_back.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MealPlanCreateResponse(
        int year,
        int month,
        String generatedAt,
        List<DayMenu> days,
        Map<String, Object> meta
) {}

