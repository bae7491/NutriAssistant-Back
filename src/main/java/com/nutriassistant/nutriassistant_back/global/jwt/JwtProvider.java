package com.nutriassistant.nutriassistant_back.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    private final Key key;
    private final long tokenValidityInMilliseconds;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration}") long tokenValidityInMilliseconds) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
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
                .setSubject(username)
                .claim("id", userId)
                .claim("schoolId", schoolId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================================================================
    // 2. 인증 정보 조회 (SecurityContext 저장용)
    // =========================================================================
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        Long id = claims.get("id", Long.class);
        String username = claims.getSubject();
        Long schoolId = claims.get("schoolId", Long.class);
        String role = claims.get("role", String.class);

        CustomUserDetails userDetails = new CustomUserDetails(id, username, role, schoolId);

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    // =========================================================================
    // 3. 토큰 유효성 검증
    // =========================================================================
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
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
    // 4. 정보 추출용 Getter (★ 누락되었던 부분 추가!)
    // =========================================================================

    // 아이디(username) 추출
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // [추가] 사용자 PK(id) 추출
    public Long getUserId(String token) {
        return parseClaims(token).get("id", Long.class);
    }

    // [추가] 학교 ID 추출
    public Long getSchoolId(String token) {
        return parseClaims(token).get("schoolId", Long.class);
    }

    // [추가] 권한(Role) 추출
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // 내부 파싱 메서드
    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}