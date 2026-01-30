package com.nutriassistant.nutriassistant_back.domain.metrics.repository;

import com.nutriassistant.nutriassistant_back.domain.metrics.entity.ReviewAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ReviewAnalysisRepository extends JpaRepository<ReviewAnalysis, Long> {

    @Query("SELECT COUNT(r) FROM ReviewAnalysis r WHERE r.schoolId = :schoolId AND r.createdAt BETWEEN :startDate AND :endDate")
    long countBySchoolIdAndDateRange(@Param("schoolId") Long schoolId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(r) FROM ReviewAnalysis r WHERE r.schoolId = :schoolId AND r.sentimentLabel = :label AND r.createdAt BETWEEN :startDate AND :endDate")
    long countBySchoolIdAndLabelAndDateRange(@Param("schoolId") Long schoolId, @Param("label") String label, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 추가적인 페이징 조회 메서드 등이 필요할 수 있음
}