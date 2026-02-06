package com.nutriassistant.nutriassistant_back.domain.review.repository;

import com.nutriassistant.nutriassistant_back.domain.review.entity.Review;
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
    // 5. [신규 추가] 중복 리뷰 방지용 존재 여부 확인
    // 학생 ID + 날짜 + 식사유형(중식 등)이 일치하는 리뷰가 있는지 검사 (true면 이미 쓴 것)
    // =================================================================
    boolean existsByStudentIdAndDateAndMealType(Long studentId, LocalDate date, String mealType);

    // 만약 MealType이 Enum이 아니라 String이라면 아래처럼 쓰세요:
    // boolean existsByStudentIdAndDateAndMealType(Long studentId, LocalDate date, String mealType);
}