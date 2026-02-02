package com.nutriassistant.nutriassistant_back.domain.reviewanalysis.repository;

import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.entity.ReviewAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewAnalysisRepository extends JpaRepository<ReviewAnalysis, Long> {

    // [1] 우리가 새로 만든 메서드 (단순 조회)
    List<ReviewAnalysis> findBySchoolId(Long schoolId);

    // [2] 우리가 새로 만든 메서드 (가장 최근 데이터 1건 조회 - Controller용)
    ReviewAnalysis findTopBySchoolIdOrderByTargetYmDesc(Long schoolId);

    // =========================================================================
    // [3] 기존 파일(metrics)에 있던 메서드들을 여기로 이사시켰습니다.
    // =========================================================================

    @Query("SELECT COUNT(r) FROM ReviewAnalysis r WHERE r.schoolId = :schoolId AND r.createdAt BETWEEN :startDate AND :endDate")
    long countBySchoolIdAndDateRange(@Param("schoolId") Long schoolId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(r) FROM ReviewAnalysis r WHERE r.schoolId = :schoolId AND r.sentimentLabel = :label AND r.createdAt BETWEEN :startDate AND :endDate")
    long countBySchoolIdAndLabelAndDateRange(@Param("schoolId") Long schoolId, @Param("label") String label, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}