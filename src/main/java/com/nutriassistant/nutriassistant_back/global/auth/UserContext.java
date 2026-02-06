package com.nutriassistant.nutriassistant_back.global.auth;

import lombok.Builder;
import lombok.Getter;

/**
 * 현재 로그인한 사용자의 인증 정보를 담는 클래스
 * JWT 구현 시 토큰에서 추출한 정보를 이 객체에 담습니다.
 */
@Getter
@Builder
public class UserContext {

    private final Long userId;
    private final Long schoolId;
    private final String role;

    public static UserContext of(Long userId, Long schoolId, String role) {
        return UserContext.builder()
                .userId(userId)
                .schoolId(schoolId)
                .role(role)
                .build();
    }

    public static UserContext guest() {
        return UserContext.builder()
                .userId(null)
                .schoolId(null)
                .role("GUEST")
                .build();
    }

    // [수정] userId를 반환하도록 구현
    public Long getId() {
        return this.userId;
    }
}