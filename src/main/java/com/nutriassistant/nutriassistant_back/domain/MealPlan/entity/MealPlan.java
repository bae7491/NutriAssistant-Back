package com.nutriassistant.nutriassistant_back.domain.MealPlan.entity;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // [삭제 완료] generated_at 필드는 제거했습니다.

    @OneToMany(
            mappedBy = "mealPlan",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MealPlanMenu> menus = new ArrayList<>();

    public MealPlan(Long schoolId, Integer year, Integer month) {
        this.schoolId = schoolId;
        this.year = year;
        this.month = month;
    }

    public void addMenu(MealPlanMenu menu) {
        menus.add(menu);
        menu.setMealPlan(this);
    }

    public void removeMenu(MealPlanMenu menu) {
        menus.remove(menu);
        menu.setMealPlan(null);
    }
}