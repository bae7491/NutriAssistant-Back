package com.nutriassistant.nutriassistant_back.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtProvider {

    private final Key key;
    private final long tokenValidityInMilliseconds;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration}") long tokenValidityInMilliseconds) {
        byte[] keyBytes = secret.getBytes();
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
    }

    // =========================================================================
    // 1. 토큰 생성
    // =========================================================================
    public String createToken(Long userId, String username, Long schoolId, String role) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.tokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(username)                 // 사용자 아이디 (sub)
                .claim("id", userId)            // 사용자 PK
                .claim("schoolId", schoolId)    // 학교 ID
                .claim("role", role)            // 권한 (ROLE_STUDENT 등)
                .setIssuedAt(new Date())
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================================================================
    // 2. 인증 정보(Authentication) 조회 (★ 필터에서 사용하므로 필수 추가)
    // =========================================================================
    public Authentication getAuthentication(String accessToken) {
        // 1. 토큰 복호화
        Claims claims = parseClaims(accessToken);

        // 2. 권한 정보 추출 (예: "ROLE_DIETITIAN")
        // 권한이 없으면 기본적으로 빈 문자열이나 예외처리를 할 수 있지만, 여기선 있다고 가정
        String role = claims.get("role", String.class);
        if (role == null) {
            role = "ROLE_UNKNOWN"; // 안전장치
        }

        // 3. Spring Security용 권한 리스트 생성
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        // 4. UserDetails 객체 생성 (Principal)
        // 비밀번호는 토큰 인증에서 필요 없으므로 빈 문자열("") 전달
        UserDetails principal = new User(claims.getSubject(), "", authorities);

        // 5. Authentication 객체 반환
        return new UsernamePasswordAuthenticationToken(principal, accessToken, authorities);
    }

    // =========================================================================
    // 3. 토큰 유효성 검증
    // =========================================================================
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    // =========================================================================
    // 4. 정보 추출용 Getter 메서드들
    // =========================================================================

    // 아이디(username) 추출
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // 사용자 PK(id) 추출
    public Long getUserId(String token) {
        return parseClaims(token).get("id", Long.class);
    }

    // 학교 ID 추출
    public Long getSchoolId(String token) {
        return parseClaims(token).get("schoolId", Long.class);
    }

    // 권한(Role) 추출
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // 내부적으로 Claims를 파싱하는 헬퍼 메서드
    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 클레임(정보)은 확인하고 싶을 때 사용
            return e.getClaims();
        }
    }
}