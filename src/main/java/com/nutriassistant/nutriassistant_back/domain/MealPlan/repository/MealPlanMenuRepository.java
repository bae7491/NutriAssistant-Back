package com.nutriassistant.nutriassistant_back.domain.MealPlan.repository;

import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.MealPlanMenu;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealPlanMenuRepository extends JpaRepository<MealPlanMenu, Long> {

    /**
     * 특정 식단표의 모든 메뉴 조회 (날짜, 식사유형 순 정렬)
     */
    List<MealPlanMenu> findByMealPlanIdOrderByMenuDateAscMealTypeAsc(Long mealPlanId);

    /**
     * 특정 식단표의 모든 메뉴 조회 (alias, 날짜, 식사유형 순 정렬)
     */
    List<MealPlanMenu> findAllByMealPlanIdOrderByMenuDateAscMealTypeAsc(Long mealPlanId);

    /**
     * 특정 식단표의 모든 메뉴 삭제
     */
    void deleteByMealPlan_Id(Long mealPlanId);

    /**
     * [단건 조회] 학교 ID, 날짜, 식사 유형으로 메뉴 조회 (예: 특정 날짜의 점심만)
     */
    Optional<MealPlanMenu> findByMealPlan_SchoolIdAndMenuDateAndMealType(
            Long schoolId, LocalDate menuDate, MealType mealType
    );

    /**
     * [다건 조회/추가됨] 학교 ID와 날짜로 모든 끼니 조회 (조식, 중식, 석식 모두 포함)
     * 용도: 오늘의 식단 전체 리스트 조회
     */
    List<MealPlanMenu> findAllByMealPlan_SchoolIdAndMenuDateOrderByMealTypeAsc(
            Long schoolId, LocalDate menuDate
    );

    /**
     * 학교 ID와 날짜 범위로 메뉴 조회 (주간 식단표 등)
     */
    List<MealPlanMenu> findByMealPlan_SchoolIdAndMenuDateBetweenOrderByMenuDateAscMealTypeAsc(
            Long schoolId, LocalDate startDate, LocalDate endDate
    );
}