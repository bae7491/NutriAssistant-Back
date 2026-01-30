package com.nutriassistant.nutriassistant_back.domain.MealPlan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "menu_cost")
@Getter
@Setter
@NoArgsConstructor
public class MenuCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String menuName;

    @Column(nullable = false)
    private Integer price; // 단가 (원)

    @Column(nullable = false)
    private Integer baseYear; // 기준 연도 (예: 2023)

    @Column(nullable = false)
    private Integer currentYear; // 현재 연도 (예: 2026)

    @Column(nullable = false)
    private Double inflationMultiplier; // 물가상승률 계수

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}