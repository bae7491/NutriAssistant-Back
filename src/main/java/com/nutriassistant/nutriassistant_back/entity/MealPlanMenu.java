package com.nutriassistant.nutriassistant_back.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "meal_plan_menu",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meal_plan_menu_plan_date_type",
                columnNames = {"meal_plan_id", "menu_date", "meal_type"}
        )
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC) // ✅ 서비스에서 new MealPlanMenu() 가능
public class MealPlanMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @Column(name = "menu_date", nullable = false)
    private LocalDate menuDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    private String rice;
    private String soup;
    private String main1;
    private String main2;
    private String side;
    private String kimchi;
    private String dessert;

    private Integer kcal;
    private Integer carb;
    private Integer prot;
    private Integer fat;
    private Integer cost;

    @Column(name = "raw_menus_json", columnDefinition = "tinytext")
    private String rawMenusJson;
}