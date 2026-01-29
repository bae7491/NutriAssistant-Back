package com.nutriassistant.nutriassistant_back.MealPlan.repository;

import com.nutriassistant.nutriassistant_back.MealPlan.entity.MenuHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuHistoryRepository extends JpaRepository<MenuHistory, Long> {

    // [추가] 최신순(ID 내림차순)으로 전체 조회
    List<MenuHistory> findAllByOrderByIdDesc();

    // 날짜로 조회
    Page<MenuHistory> findByMealDateOrderByIdDesc(String mealDate, Pageable pageable);

    // 날짜 + 식사유형으로 조회
    Page<MenuHistory> findByMealDateAndMealTypeOrderByIdDesc(String mealDate, String mealType, Pageable pageable);

    // 날짜 + 액션타입으로 조회
    Page<MenuHistory> findByMealDateAndActionTypeOrderByIdDesc(String mealDate, MenuHistory.ActionType actionType, Pageable pageable);

    // 날짜 + 식사유형 + 액션타입으로 조회
    Page<MenuHistory> findByMealDateAndMealTypeAndActionTypeOrderByIdDesc(
            String mealDate, String mealType, MenuHistory.ActionType actionType, Pageable pageable);

    // 식사유형으로 조회
    Page<MenuHistory> findByMealTypeOrderByIdDesc(String mealType, Pageable pageable);

    // 액션타입으로 조회
    Page<MenuHistory> findByActionTypeOrderByIdDesc(MenuHistory.ActionType actionType, Pageable pageable);

    // 식사유형 + 액션타입으로 조회
    Page<MenuHistory> findByMealTypeAndActionTypeOrderByIdDesc(String mealType, MenuHistory.ActionType actionType, Pageable pageable);

    // 전체 조회 (페이징)
    Page<MenuHistory> findAllByOrderByIdDesc(Pageable pageable);
}