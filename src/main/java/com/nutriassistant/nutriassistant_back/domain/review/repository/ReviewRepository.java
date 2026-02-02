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
    // 용도: 단순 날짜별 조회
    List<Review> findAllBySchoolIdAndDate(Long schoolId, LocalDate date);

    // 2. [기존] 학생별 리뷰 조회
    // 용도: '내 리뷰 보기' 기능
    List<Review> findAllByStudentId(Long studentId);

    // 3. [통합/수정] 특정 학교, 특정 시간대(Start~End) 사이의 리뷰 조회
    // 용도: 일일/월간 AI 분석 및 통계 집계용 (MetricsService에서 사용)
    // 참고: Spring Data JPA에서 'find...'와 'findAll...'은 동일하게 동작합니다.
    //      Service 코드와 맞추기 위해 findBy...로 통일합니다.
    List<Review> findBySchoolIdAndCreatedAtBetween(Long schoolId, LocalDateTime start, LocalDateTime end);

    // 4. [추가] 학교별 리뷰 목록 조회 (페이징 지원)
    // 용도: 만족도 리뷰 목록 API (GET /metrics/satisfaction/reviews)에서 사용
    // Pageable을 파라미터로 넘기면 자동으로 LIMIT, OFFSET 쿼리가 생성됩니다.
    Page<Review> findBySchoolId(Long schoolId, Pageable pageable);
}