package com.nutriassistant.nutriassistant_back.domain.review.repository;

import com.nutriassistant.nutriassistant_back.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 학교, 특정 날짜의 리뷰 조회 (나중에 AI 분석 배치가 가져갈 때 사용)
    List<Review> findAllBySchoolIdAndDate(Long schoolId, LocalDate date);

    // 학생별 리뷰 조회 (내 리뷰 보기)
    List<Review> findAllByStudentId(Long studentId);
}