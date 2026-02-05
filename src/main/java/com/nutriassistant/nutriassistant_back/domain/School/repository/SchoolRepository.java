package com.nutriassistant.nutriassistant_back.domain.School.repository;

import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {

    // ✅ 영양사 ID로 학교 조회 (1:1 관계이므로 Optional)
    Optional<School> findByDietitian_Id(Long dietitianId);

    // ✅ 학교 코드로 학교 조회 (중복 가입 방지 및 기존 학교 존재 여부 확인)
    Optional<School> findBySchoolCode(String schoolCode);
}