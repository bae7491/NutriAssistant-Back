package com.nutriassistant.nutriassistant_back.Auth.repository;

import com.nutriassistant.nutriassistant_back.Auth.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    // ✅ (school_id + username) 기준 중복 체크 (현재 서비스 로직과 일치)
    boolean existsBySchoolIdAndUsername(Long schoolId, String username);

    Optional<Student> findBySchoolIdAndUsername(Long schoolId, String username);

    // ✅ (선택) 만약 username을 전역 유니크로 쓰고 싶을 때 대비
    boolean existsByUsername(String username);

    Optional<Student> findByUsername(String username);
}
