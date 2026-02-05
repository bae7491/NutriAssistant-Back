package com.nutriassistant.nutriassistant_back.domain.Auth.repository;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DietitianRepository extends JpaRepository<Dietitian, Long> {

    // ✅ 회원가입 중복 체크용
    boolean existsByUsername(String username);

    // ✅ 로그인용
    Optional<Dietitian> findByUsername(String username);

    // ✅ [추가] 아이디 찾기용 (이름 + 전화번호)
    Optional<Dietitian> findByNameAndPhone(String name, String phone);

    // ✅ [추가] 비밀번호 찾기용 (아이디 + 이름 + 전화번호)
    Optional<Dietitian> findByUsernameAndNameAndPhone(String username, String name, String phone);
}