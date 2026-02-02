package com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AllergenInfoResponse {

    @JsonProperty("student_id")
    private Long studentId;

    @JsonProperty("selected_allergens")
    private List<Integer> selectedAllergens;

    @JsonProperty("selected_allergen_names")
    private List<String> selectedAllergenNames;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime updatedAt;
}
