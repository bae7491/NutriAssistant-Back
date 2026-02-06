package com.nutriassistant.nutriassistant_back.domain.Auth.repository;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;
import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    // ✅ 회원가입 중복 체크용
    boolean existsBySchoolAndUsername(School school, String username);
    boolean existsByPhone(String phone);

    // ✅ 로그인용
    Optional<Student> findByUsername(String username);

    // ✅ [아이디 찾기] 이름 + 전화번호로 조회
    Optional<Student> findByNameAndPhone(String name, String phone);

    // ✅ [비밀번호 찾기] 아이디(이메일) + 이름 + 전화번호로 조회 (수정됨)
    Optional<Student> findByUsernameAndNameAndPhone(String username, String name, String phone);
}