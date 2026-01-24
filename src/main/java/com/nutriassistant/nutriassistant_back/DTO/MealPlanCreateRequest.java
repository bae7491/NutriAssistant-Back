package com.nutriassistant.nutriassistant_back.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record MealPlanCreateRequest(
        int year,
        int month,
        LocalDateTime generatedAt,

        // FastAPI는 "meals"로 보내지만, 내부적으로 menus로 사용
        @JsonProperty("meals")  // JSON에서는 "meals"로 받음
        com.fasterxml.jackson.databind.JsonNode menus    // 코드에서는 menus로 사용
) {
    public record MealMenu(
            @JsonProperty("Date") String date,
            @JsonProperty("Type") String type,
            @JsonProperty("Rice") String rice,
            @JsonProperty("Soup") String soup,
            @JsonProperty("Main1") String main1,
            @JsonProperty("Main2") String main2,
            @JsonProperty("Side") String side,
            @JsonProperty("Kimchi") String kimchi,
            @JsonProperty("Dessert") String dessert,
            @JsonProperty("RawMenus") List<String> rawMenus,
            @JsonProperty("Kcal") Integer kcal,
            @JsonProperty("Carb") Integer carb,
            @JsonProperty("Prot") Integer prot,
            @JsonProperty("Fat") Integer fat,
            @JsonProperty("Cost") Integer cost
    ) {}
}