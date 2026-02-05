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

    private Long id;           // DB PK (User ID)
    private String username;   // 로그인 ID
    private String password;   // 암호화된 비밀번호
    private String role;       // ROLE_USER or ROLE_DIETITIAN

    // ▼ [추가됨] 학교 정보 조회를 위해 필수!
    private Long schoolId;

    // =========================================================================
    // 생성자 1: 영양사(Dietitian) 로그인 시
    // =========================================================================
    public CustomUserDetails(Dietitian dietitian) {
        this.id = dietitian.getId();
        this.username = dietitian.getUsername();
        this.password = dietitian.getPasswordHash(); // Dietitian 엔티티 필드명 확인 필요 (pw인지 passwordHash인지)
        this.role = "ROLE_DIETITIAN";

        // 영양사는 School 엔티티를 통해 schoolId를 가져와야 함 (1:1 관계 가정)
        // 주의: 영양사가 아직 학교를 등록 안 했으면 null일 수 있음
        // FetchType.LAZY 문제 방지를 위해 서비스단에서 School ID를 넘겨주는 것이 안전함
        // 일단은 null로 두고, 로그인 시점에 채워넣는 로직이 필요할 수 있음.
    }

    // ▼ [추천] 영양사용 생성자 (SchoolId 직접 주입)
    public CustomUserDetails(Dietitian dietitian, Long schoolId) {
        this.id = dietitian.getId();
        this.username = dietitian.getUsername();
        this.password = dietitian.getPasswordHash();
        this.role = "ROLE_DIETITIAN";
        this.schoolId = schoolId;
    }

    // =========================================================================
    // 생성자 2: 학생(Student) 로그인 시
    // =========================================================================
    public CustomUserDetails(Student student) {
        this.id = student.getId();
        this.username = student.getUsername();
        this.password = student.getPasswordHash();
        this.role = "ROLE_STUDENT"; // ROLE_USER 대신 구체적으로

        // ▼ [추가] 학생은 무조건 학교에 소속되어 있음
        if (student.getSchool() != null) {
            this.schoolId = student.getSchool().getId();
        }
    }

    // =========================================================================
    // 생성자 3: 토큰 파싱용 (가장 중요 ★)
    // JwtProvider가 토큰을 깔 때 이 생성자를 씁니다.
    // =========================================================================
    public CustomUserDetails(Long id, String username, String role, Long schoolId) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.schoolId = schoolId;
    }

    // (나머지 Override 메서드는 그대로 유지)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}