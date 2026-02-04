package com.nutriassistant.nutriassistant_back.domain.School.repository;

import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {

    // [수정] findByDietitian_Id (School 엔티티의 필드명이 dietitian이어야 함)
    Optional<School> findByDietitian_Id(Long dietitianId);


    // 학교 중복 가입 방지용
    Optional<School> findBySchoolCode(String schoolCode);
}