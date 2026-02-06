package com.nutriassistant.nutriassistant_back.domain.MealPlan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor // [수정] protected 제거 -> public으로 변경 (에러 해결 핵심)
@AllArgsConstructor
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @Column(name = "menu_date", nullable = false)
    private LocalDate menuDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 16)
    private MealType mealType;

    @Column(name = "rice_display", length = 255)
    private String riceDisplay;

    @Column(name = "soup_display", length = 255)
    private String soupDisplay;

    @Column(name = "main1_display", length = 255)
    private String main1Display;

    @Column(name = "main2_display", length = 255)
    private String main2Display;

    @Column(name = "side_display", length = 255)
    private String sideDisplay;

    @Column(name = "kimchi_display", length = 255)
    private String kimchiDisplay;

    @Column(name = "dessert_display", length = 255)
    private String dessertDisplay;

    @Column(precision = 10, scale = 2)
    private BigDecimal kcal;

    @Column(precision = 10, scale = 2)
    private BigDecimal carb;

    @Column(precision = 10, scale = 2)
    private BigDecimal prot;

    @Column(precision = 10, scale = 2)
    private BigDecimal fat;

    @Column
    private Integer cost;

    @Column(name = "ai_comment", length = 255)
    private String aiComment;

    @Column(name = "raw_menus_json", columnDefinition = "TEXT")
    private String rawMenusJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 기존 서비스 코드와의 호환성을 위한 생성자
    public MealPlanMenu(LocalDate menuDate, MealType mealType) {
        this.menuDate = menuDate;
        this.mealType = mealType;
    }
}