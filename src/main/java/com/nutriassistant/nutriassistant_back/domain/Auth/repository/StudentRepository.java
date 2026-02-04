package com.nutriassistant.nutriassistant_back.domain.Auth.repository;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;
import com.nutriassistant.nutriassistant_back.domain.School.entity.School; // ✅ School 엔티티 임포트 필수
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * ✅ 중복 체크 수정 (Long schoolId -> School school)
     * AuthService에서 이미 School 객체를 조회했으므로, 그 객체를 그대로 넘겨주면 됩니다.
     * SQL 실행 시 자동으로: where student.school_id = ? AND student.username = ? 로 변환됩니다.
     */
    boolean existsBySchoolAndUsername(School school, String username);

    /**
     * ✅ 특정 학교의 특정 학생 조회 (필요한 경우)
     */
    Optional<Student> findBySchoolAndUsername(School school, String username);

    /**
     * ✅ 로그인용 조회 (아이디로만 찾기)
     * username이 서비스 전체에서 유니크하다면 이것만 있어도 로그인이 가능합니다.
     */
    Optional<Student> findByUsername(String username);

    // existsByUsername 등은 필요에 따라 유지하거나 삭제하시면 됩니다.
}