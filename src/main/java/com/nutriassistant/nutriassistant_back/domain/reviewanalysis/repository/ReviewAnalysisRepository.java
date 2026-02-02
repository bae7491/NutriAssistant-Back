package com.nutriassistant.nutriassistant_back.domain.reviewanalysis.repository;

import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.entity.ReviewAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewAnalysisRepository extends JpaRepository<ReviewAnalysis, Long> {
    List<ReviewAnalysis> findBySchoolId(Long schoolId);

    ReviewAnalysis findTopBySchoolIdOrderByTargetYmDesc(Long schoolId);
}