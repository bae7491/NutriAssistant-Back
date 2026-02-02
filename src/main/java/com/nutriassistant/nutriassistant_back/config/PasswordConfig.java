package com.nutriassistant.nutriassistant_back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 암호화 설정 클래스
 *
 * 역할:
 * - 사용자 비밀번호를 안전하게 암호화하기 위한 인코더를 제공합니다.
 * - 회원가입 시 비밀번호 암호화, 로그인 시 비밀번호 검증에 사용됩니다.
 *
 * BCrypt 알고리즘 특징:
 * - 단방향 해시 함수 (복호화 불가능)
 * - 솔트(Salt) 자동 생성 (레인보우 테이블 공격 방지)
 * - 강도(Strength) 조절 가능 (기본값: 10)
 * - 같은 비밀번호도 매번 다른 해시값 생성
 *
 * 사용 예시:
 * <pre>
 * // 비밀번호 암호화 (회원가입 시)
 * String encodedPassword = passwordEncoder.encode("rawPassword");
 *
 * // 비밀번호 검증 (로그인 시)
 * boolean isMatch = passwordEncoder.matches("rawPassword", encodedPassword);
 * </pre>
 *
 * 보안 권장사항:
 * - 비밀번호는 반드시 암호화하여 저장
 * - 원본 비밀번호는 절대 로그에 출력하지 않음
 * - 비밀번호 정책 적용 (최소 길이, 특수문자 포함 등)
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt 비밀번호 인코더 Bean
     *
     * @return BCryptPasswordEncoder 인스턴스
     *
     * 강도(Strength) 설정:
     * - 기본값: 10 (2^10 = 1024회 해싱)
     * - 값이 높을수록 보안성 증가, 하지만 성능 저하
     * - 권장 범위: 10~12
     *
     * 커스터마이징 예시:
     * return new BCryptPasswordEncoder(12); // 강도 12로 설정
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
