package com.nutriassistant.nutriassistant_back.repository;

import com.nutriassistant.nutriassistant_back.entity.MealPlanMenu;
import com.nutriassistant.nutriassistant_back.entity.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealPlanMenuRepository extends JpaRepository<MealPlanMenu, Long> {

    // 1. JPQL 쿼리 (Service의 findByDateAndType에서 사용)
    @Query("SELECT m FROM MealPlanMenu m WHERE m.menuDate = :date AND m.mealType = :mealType")
    Optional<MealPlanMenu> findByDateAndType(@Param("date") LocalDate date, @Param("mealType") MealType mealType);

    // 2. [추가됨] Service의 getOne에서 사용 (최신순 1건 조회)
    Optional<MealPlanMenu> findFirstByMenuDateAndMealTypeOrderByMenuDateDescIdDesc(LocalDate menuDate, MealType mealType);

    // 3. [추가됨] Service의 importFromFastApi에서 사용 (기존 데이터 삭제)
    void deleteByMealPlan_Id(Long mealPlanId);

    // 4. Controller에서 사용 (특정 식단 ID로 전체 조회)
    List<MealPlanMenu> findAllByMealPlanId(Long mealPlanId);
}