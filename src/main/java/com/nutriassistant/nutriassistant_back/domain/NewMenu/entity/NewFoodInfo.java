package com.nutriassistant.nutriassistant_back.domain.NewMenu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "new_food_info")
@Getter
@Setter
@NoArgsConstructor
public class NewFoodInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

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

    @Column(name = "vitamin_a", precision = 10, scale = 2)
    private BigDecimal vitaminA;

    @Column(name = "thiamin", precision = 38, scale = 2)
    private BigDecimal thiamin;

    @Column(name = "riboflavin", precision = 38, scale = 2)
    private BigDecimal riboflavin;

    @Column(name = "vitamin_c", precision = 10, scale = 2)
    private BigDecimal vitaminC;

    @Lob
    @Column(name = "ingredients", columnDefinition="LONGTEXT")
    private String ingredients;

    @Column(name = "allergy_info")
    private String allergyInfo;

    @Lob
    @Column(name="recipe", columnDefinition="LONGTEXT")
    private String recipe;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
