package com.nutriassistant.nutriassistant_back.MealPlan.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meal_plan")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")  // ✅ 실제 DB 컬럼명에 맞게 변경
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealPlanMenu> menus = new ArrayList<>();

    public MealPlan(Integer year, Integer month, LocalDateTime generatedAt) {
        this.year = year;
        this.month = month;
        this.generatedAt = generatedAt;
    }
}