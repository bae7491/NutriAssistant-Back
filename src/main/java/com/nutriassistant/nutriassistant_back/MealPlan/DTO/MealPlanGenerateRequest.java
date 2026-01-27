package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MealPlanGenerateRequest {
    private Integer year;
    private Integer month;
    private OptionsDto options;
    private JsonNode report;
}