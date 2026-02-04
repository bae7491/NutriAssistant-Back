package com.nutriassistant.nutriassistant_back.domain.Auth.repository;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DietitianRepository extends JpaRepository<Dietitian, Long> {

    // 아이디 중복 체크용
    boolean existsByUsername(String username);

    // 로그인 및 학교 등록 시 사용
    Optional<Dietitian> findByUsername(String username);
}