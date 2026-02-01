package com.nutriassistant.nutriassistant_back.domain.Menu.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class FoodInfoDetailResponse {

    @JsonProperty("menu_id")
    private String menuId;

    private String name;

    private String category;

    @JsonProperty("nutrition_basis")
    private String nutritionBasis;

    @JsonProperty("serving_size")
    private String servingSize;

    private Integer kcal;

    private BigDecimal carb;

    private BigDecimal prot;

    private BigDecimal fat;

    private BigDecimal calcium;

    private BigDecimal iron;

    @JsonProperty("vitamin_a")
    private BigDecimal vitaminA;

    private BigDecimal thiamin;

    private BigDecimal riboflavin;

    @JsonProperty("vitamin_c")
    private BigDecimal vitaminC;

    @JsonProperty("ingredients_text")
    private String ingredientsText;

    private List<Integer> allergens;

    @JsonProperty("recipe_text")
    private String recipeText;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}
