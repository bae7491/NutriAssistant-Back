package com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class NewFoodInfoCreateRequest {

    @NotBlank(message = "메뉴명은 필수입니다.")
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
}
