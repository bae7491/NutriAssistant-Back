package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.repository;

import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.entity.MonthlyOpsDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyOpsDocRepository extends JpaRepository<MonthlyOpsDoc, Long> {

    // 중복 생성 방지 체크용
    boolean existsBySchoolIdAndYearAndMonth(Long schoolId, Integer year, Integer month);

    // 학교별 목록 조회 (페이징)
    Page<MonthlyOpsDoc> findAllBySchoolId(Long schoolId, Pageable pageable);

    // [MealPlanService 연동용]
    // 특정 연/월의 리포트 조회 (주의: 학교 구분 없이 연/월로만 찾으므로, 데이터가 유니크해야 안전함)
    // 실제 운영 환경에서는 findBySchoolIdAndYearAndMonth 권장
    Optional<MonthlyOpsDoc> findByYearAndMonth(Integer year, Integer month);
}