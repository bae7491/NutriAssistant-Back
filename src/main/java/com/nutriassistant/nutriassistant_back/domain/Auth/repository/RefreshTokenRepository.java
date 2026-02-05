package com.nutriassistant.nutriassistant_back.domain.Auth.repository;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // 로그아웃/탈퇴 시 해당 유저의 토큰 삭제
    void deleteByUsername(String username);

    // 토큰 검증용 (필요 시 사용)
    Optional<RefreshToken> findByToken(String token);
}