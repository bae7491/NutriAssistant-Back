package com.nutriassistant.nutriassistant_back.domain.Auth.repository;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DietitianRepository extends JpaRepository<Dietitian, Long> {

    // ✅ 회원가입 중복 체크용
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    // ✅ 로그인용
    Optional<Dietitian> findByUsername(String username);

    // -----------------------------------------------------------------
    // ⬇️ 아래 메서드들이 AuthService의 영양사 찾기 로직과 매칭됩니다.
    // -----------------------------------------------------------------

    // ✅ [아이디 찾기] 이름 + 이메일로 조회
    Optional<Dietitian> findByNameAndEmail(String name, String email);

    // ✅ [비밀번호 찾기] 아이디 + 이름 + 이메일로 조회
    Optional<Dietitian> findByUsernameAndNameAndEmail(String username, String name, String email);

    // (참고) 만약 전화번호로 찾는 기존 로직을 유지한다면 아래도 남겨두세요.
    // Optional<Dietitian> findByNameAndPhone(String name, String phone);
    // Optional<Dietitian> findByUsernameAndNameAndPhone(String username, String name, String phone);
}