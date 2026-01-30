package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.repository;

import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.entity.MonthlyOpsDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyOpsDocRepository extends JpaRepository<MonthlyOpsDoc, Long> {
    boolean existsBySchoolIdAndYearAndMonth(Long schoolId, Integer year, Integer month);
    Page<MonthlyOpsDoc> findAllBySchoolId(Long schoolId, Pageable pageable);
}