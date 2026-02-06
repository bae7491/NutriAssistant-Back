package com.nutriassistant.nutriassistant_back.domain.review.repository;

import com.nutriassistant.nutriassistant_back.domain.review.entity.Review;
// [중요] 식단 도메인의 MealType Enum을 꼭 import 해야 합니다!
import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.MealType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 1. [기존] 특정 학교, 특정 날짜(LocalDate)의 리뷰 조회
    List<Review> findAllBySchoolIdAndDate(Long schoolId, LocalDate date);

    // 2. [기존] 학생별 리뷰 조회
    List<Review> findAllByStudentId(Long studentId);

    // 3. [기존] 통계용 조회
    List<Review> findBySchoolIdAndCreatedAtBetween(Long schoolId, LocalDateTime start, LocalDateTime end);

    // 4. [기존] 페이징 조회
    Page<Review> findBySchoolId(Long schoolId, Pageable pageable);

    // =================================================================
    // 5. [수정됨] 중복 리뷰 방지용 존재 여부 확인
    // 중요: 파라미터 타입을 String -> MealType으로 변경했습니다.
    // =================================================================
    boolean existsByStudentIdAndDateAndMealType(Long studentId, LocalDate date, MealType mealType);
}