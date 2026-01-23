package com.nutriassistant.nutriassistant_back.repository;

import java.time.LocalDate;
import java.util.Optional;

import com.nutriassistant.nutriassistant_back.entity.MealType;

import com.nutriassistant.nutriassistant_back.entity.MealPlanMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealPlanMenuRepository extends JpaRepository<MealPlanMenu, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MealPlanMenu m where m.mealPlan.id = :mealPlanId")
    void deleteByMealPlan_Id(@Param("mealPlanId") Long mealPlanId);

    Optional<MealPlanMenu> findFirstByMenuDateAndMealTypeOrderByMenuDateDescIdDesc(
            LocalDate menuDate, MealType mealType
    );
}