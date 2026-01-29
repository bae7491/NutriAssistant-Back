package com.nutriassistant.nutriassistant_back.MealPlan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "meal_plan_menu",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_meal_plan_menu_plan_date_type",
                        columnNames = {"meal_plan_id", "menu_date", "meal_type"}
                )
        },
        indexes = {
                @Index(name = "idx_meal_plan_menu_plan_date", columnList = "meal_plan_id,menu_date"),
                @Index(name = "idx_meal_plan_menu_date_type", columnList = "menu_date,meal_type")
        }
)
public class MealPlanMenu {

    // ====== getters/setters ======
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 일간 식단표 Id

    // ====== setter for relation ======
    // many daily rows belong to one monthly plan
    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan; // 식단표 Id

    @Setter
    @Column(name = "menu_date", nullable = false)
    private LocalDate menuDate; // 메뉴 날짜

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 16)
    private MealType mealType; // 중/석식 구분

    // display strings (may include allergens like "(1,2,5)")
    @Setter
    @Column(name = "rice_display", length = 255)
    private String riceDisplay; // 밥

    @Setter
    @Column(name = "soup_display", length = 255)
    private String soupDisplay; // 국

    @Setter
    @Column(name = "main1_display", length = 255)
    private String main1Display; // 주찬1

    @Setter
    @Column(name = "main2_display", length = 255)
    private String main2Display; // 주찬2

    @Setter
    @Column(name = "side_display", length = 255)
    private String sideDisplay; // 부찬

    @Setter
    @Column(name = "kimchi_display", length = 255)
    private String kimchiDisplay; // 김치

    @Setter
    @Column(name = "dessert_display", length = 255)
    private String dessertDisplay; // 후식

    @Setter
    @Column(precision = 10, scale = 2)
    private BigDecimal kcal; // 에너지 (칼로리)

    @Setter
    @Column(precision = 10, scale = 2)
    private BigDecimal carb; // 탄수화물

    @Setter
    @Column(precision = 10, scale = 2)
    private BigDecimal prot; // 단백질

    @Setter
    @Column(precision = 10, scale = 2)
    private BigDecimal fat; // 지방

    @Setter
    @Column
    private Integer cost; // 단가

    @Setter
    @Column(name = "ai_comment", length = 255)
    private String aiComment; // 수정 사유

    @Setter
    @Column(name = "raw_menus_json", columnDefinition = "TEXT")
    private String rawMenusJson; // 원본 메뉴 JSON

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MealPlanMenu() {}

    public MealPlanMenu(LocalDate menuDate, MealType mealType) {
        this.menuDate = menuDate;
        this.mealType = mealType;
    }

}