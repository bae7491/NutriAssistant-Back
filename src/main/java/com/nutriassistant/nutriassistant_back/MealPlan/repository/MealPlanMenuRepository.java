package com.nutriassistant.nutriassistant_back.MealPlan.repository;

import com.nutriassistant.nutriassistant_back.MealPlan.entity.MealPlanMenu;
import com.nutriassistant.nutriassistant_back.MealPlan.entity.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealPlanMenuRepository extends JpaRepository<MealPlanMenu, Long> {

    /**
     * 특정 식단표의 모든 메뉴 조회
     */
    List<MealPlanMenu> findByMealPlanId(Long mealPlanId);

    /**
     * 특정 식단표의 모든 메뉴 조회 (alias)
     */
    List<MealPlanMenu> findAllByMealPlanId(Long mealPlanId);

    /**
     * 특정 식단표의 모든 메뉴 삭제
     */
    void deleteByMealPlan_Id(Long mealPlanId);

    /**
     * 학교 ID, 날짜, 식사 유형으로 메뉴 조회
     */
    Optional<MealPlanMenu> findByMealPlan_SchoolIdAndMenuDateAndMealType(
            Long schoolId, LocalDate menuDate, MealType mealType
    );

    /**
     * 학교 ID와 날짜 범위로 메뉴 조회
     */
    List<MealPlanMenu> findByMealPlan_SchoolIdAndMenuDateBetweenOrderByMenuDateAscMealTypeAsc(
            Long schoolId, LocalDate startDate, LocalDate endDate
    );
}