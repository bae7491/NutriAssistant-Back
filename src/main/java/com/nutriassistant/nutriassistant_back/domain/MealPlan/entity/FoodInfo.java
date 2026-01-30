package com.nutriassistant.nutriassistant_back.domain.MealPlan.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "food_info")
@Getter
@Setter
public class FoodInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "food_code", unique = true)
    private String foodCode;

    @Column(name = "food_name")
    private String foodName;

    @Column(name = "category")
    private String category;

    @Column(name = "serving_basis")
    private String servingBasis;

    @Column(name = "food_weight")
    private String foodWeight;

    @Column(name = "kcal")
    private Integer kcal;

    @Column(name = "protein", precision = 38, scale = 2)
    private BigDecimal protein;

    @Column(name = "fat", precision = 38, scale = 2)
    private BigDecimal fat;

    @Column(name = "carbs", precision = 38, scale = 2)
    private BigDecimal carbs;

    @Column(name = "calcium", precision = 38, scale = 2)
    private BigDecimal calcium;

    @Column(name = "iron", precision = 38, scale = 2)
    private BigDecimal iron;

    // DB column: vitamin_a (decimal(10,2))
    @Column(name = "vitamin_a", precision = 10, scale = 2)
    private BigDecimal vitaminA;

    @Column(name = "thiamin", precision = 38, scale = 2)
    private BigDecimal thiamin;

    @Column(name = "riboflavin", precision = 38, scale = 2)
    private BigDecimal riboflavin;

    // DB column: vitamin_c (decimal(10,2))
    @Column(name = "vitamin_c", precision = 10, scale = 2)
    private BigDecimal vitaminC;

    @Lob
    @Column(name = "ingredients")
    private String ingredients;

    @Column(name = "allergy_info")
    private String allergyInfo;

    @Lob
    @Column(name = "recipe")
    private String recipe;

    // DB: datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
    // DB가 자동으로 세팅/업데이트하도록 두려면 insertable/updatable을 false로 둔다.
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;
}