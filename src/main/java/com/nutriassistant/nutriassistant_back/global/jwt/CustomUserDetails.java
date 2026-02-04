package com.nutriassistant.nutriassistant_back.global.jwt;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {

    private Long id;           // DB PK
    private String username;   // 로그인 ID
    private String password;   // 암호화된 비밀번호
    private String role;       // ROLE_USER or ROLE_DIETITIAN

    // =========================================================================
    // 생성자 1: 영양사(Dietitian) 로그인 시 사용
    // =========================================================================
    public CustomUserDetails(Dietitian dietitian) {
        this.id = dietitian.getId();
        this.username = dietitian.getUsername();
        this.password = dietitian.getPw(); // Dietitian은 필드명이 pw
        this.role = "ROLE_DIETITIAN";
    }

    // =========================================================================
    // 생성자 2: 학생(Student) 로그인 시 사용 (수정됨)
    // =========================================================================
    public CustomUserDetails(Student student) {
        this.id = student.getId();
        this.username = student.getUsername();
        this.password = student.getPasswordHash(); // Student는 필드명이 passwordHash
        this.role = "ROLE_USER";
    }

    // =========================================================================
    // 생성자 3: 토큰 등에서 직접 만들 때 (ID와 권한만 필요할 때 유용)
    // =========================================================================
    public CustomUserDetails(Long id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    // ★ 컨트롤러에서 userDetails.getId()를 쓰기 위해 꼭 필요합니다!
    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}