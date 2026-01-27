package com.nutriassistant.nutriassistant_back.MealPlan.repository;

import com.nutriassistant.nutriassistant_back.MealPlan.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    Optional<MealPlan> findByYearAndMonth(Integer year, Integer month);

    // ✅ menus를 함께 가져오는 쿼리
    @Query("SELECT mp FROM MealPlan mp LEFT JOIN FETCH mp.menus WHERE mp.id = :id")
    Optional<MealPlan> findByIdWithMenus(@Param("id") Long id);
}