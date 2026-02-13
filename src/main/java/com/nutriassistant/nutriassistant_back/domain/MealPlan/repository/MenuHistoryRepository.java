package com.nutriassistant.nutriassistant_back.domain.MealPlan.repository;

import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.MenuHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuHistoryRepository extends JpaRepository<MenuHistory, Long> {

    // 학교별 날짜 범위로 조회
    Page<MenuHistory> findBySchoolIdAndMealDateBetweenOrderByIdDesc(
            Long schoolId, String startDate, String endDate, Pageable pageable);

    // 학교별 날짜 범위 + 액션타입으로 조회
    Page<MenuHistory> findBySchoolIdAndMealDateBetweenAndActionTypeOrderByIdDesc(
            Long schoolId, String startDate, String endDate, MenuHistory.ActionType actionType, Pageable pageable);

    // 학교별 액션타입으로 조회
    Page<MenuHistory> findBySchoolIdAndActionTypeOrderByIdDesc(
            Long schoolId, MenuHistory.ActionType actionType, Pageable pageable);

    // 학교별 전체 조회 (페이징)
    Page<MenuHistory> findBySchoolIdOrderByIdDesc(Long schoolId, Pageable pageable);

    // 학교별 날짜 범위 전체 조회 (월간 응답용)
    List<MenuHistory> findBySchoolIdAndMealDateBetweenOrderByIdDesc(
            Long schoolId, String startDate, String endDate);
}