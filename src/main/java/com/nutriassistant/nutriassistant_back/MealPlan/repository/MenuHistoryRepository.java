package com.nutriassistant.nutriassistant_back.MealPlan.repository;

import com.nutriassistant.nutriassistant_back.MealPlan.entity.MenuHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuHistoryRepository extends JpaRepository<MenuHistory, Long> {

    // 날짜 범위로 조회
    Page<MenuHistory> findByMealDateBetweenOrderByIdDesc(String startDate, String endDate, Pageable pageable);

    // 날짜 범위 + 액션타입으로 조회
    Page<MenuHistory> findByMealDateBetweenAndActionTypeOrderByIdDesc(
            String startDate, String endDate, MenuHistory.ActionType actionType, Pageable pageable);

    // 액션타입으로 조회
    Page<MenuHistory> findByActionTypeOrderByIdDesc(MenuHistory.ActionType actionType, Pageable pageable);

    // 전체 조회 (페이징)
    Page<MenuHistory> findAllByOrderByIdDesc(Pageable pageable);
}