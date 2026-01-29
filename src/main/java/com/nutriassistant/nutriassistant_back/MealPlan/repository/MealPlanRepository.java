package com.nutriassistant.nutriassistant_back.MealPlan.repository;

import com.nutriassistant.nutriassistant_back.MealPlan.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    Optional<MealPlan> findByYearAndMonth(Integer year, Integer month);

//    // ✅ menus를 함께 가져오는 쿼리
//    @Query("SELECT mp FROM MealPlan mp LEFT JOIN FETCH mp.menus WHERE mp.id = :id")
//    Optional<MealPlan> findByIdWithMenus;

    /**
     * 학교 ID, 연도, 월로 식단표 조회
     */
    Optional<MealPlan> findBySchoolIdAndYearAndMonth(Long schoolId, Integer year, Integer month);

    /**
     * 해당 학교의 연도/월 식단표 존재 여부 확인
     */
    boolean existsBySchoolIdAndYearAndMonth(Long schoolId, Integer year, Integer month);
}