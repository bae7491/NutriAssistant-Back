package com.nutriassistant.nutriassistant_back.MealPlan.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "meal_plan",
        // 유니크 제약 (중복 방지)
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_meal_plan_school_year_month", columnNames = {"school_id", "year", "month"})
        },
        indexes = {
                @Index(name = "idx_meal_plan_school_year_month", columnList = "school_id,year,month")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealPlan {

    // ====== getters/setters ======
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 월간 식단표 Id

    @Column(name = "school_id", nullable = false)
    private Long schoolId; // 학교 Id

    @Column(nullable = false)
    private Integer year; // 연도

    @Column(nullable = false)
    private Integer month; // 월

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 수정일

    // 일간 식단과의 관계 - 1:N 관계
    @OneToMany(
            mappedBy = "mealPlan",
            cascade = CascadeType.ALL, // 부모 조작 시, 자식 자동 반영.
            orphanRemoval = true // 부모 컬렉션에서 빠지면 DB에서도 삭제
    )
    private List<MealPlanMenu> menus = new ArrayList<>();

    public MealPlan(Long schoolId, Integer year, Integer month) {
        this.schoolId = schoolId;
        this.year = year;
        this.month = month;
    }

    // ====== convenience ======
    public void addMenu(MealPlanMenu menu) {
        menus.add(menu);
        menu.setMealPlan(this);
    }

    public void removeMenu(MealPlanMenu menu) {
        menus.remove(menu);
        menu.setMealPlan(null);
    }

}