package com.nutriassistant.nutriassistant_back.MealPlan.repository;

import com.nutriassistant.nutriassistant_back.MealPlan.entity.MenuHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuHistoryRepository extends JpaRepository<MenuHistory, Long> {

    // [추가] 최신순(ID 내림차순)으로 전체 조회
    List<MenuHistory> findAllByOrderByIdDesc();
}