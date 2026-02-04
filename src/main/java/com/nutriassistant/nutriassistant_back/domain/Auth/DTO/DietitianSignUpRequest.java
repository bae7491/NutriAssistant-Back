package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 영양사 회원가입 요청 DTO
 * * 수정사항:
 * - getSchool() 메서드가 비어있던 문제를 해결했습니다. (schoolInfo를 반환하도록 수정)
 */
public class DietitianSignUpRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
    private String pw;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;

    // JSON에서는 "school_info"로 들어오지만, 자바에서는 schoolInfo 변수에 매핑됨
    @Valid
    @NotNull(message = "학교 정보는 필수입니다.")
    @JsonProperty("school_info")
    private SchoolRequest schoolInfo;

    public DietitianSignUpRequest() {}

    // =========================================================================
    // Getters & Setters
    // =========================================================================

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPw() { return pw; }
    public void setPw(String pw) { this.pw = pw; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // 기본 Getter
    public SchoolRequest getSchoolInfo() { return schoolInfo; }
    public void setSchoolInfo(SchoolRequest schoolInfo) { this.schoolInfo = schoolInfo; }

    /**
     * [수정] Service 계층에서 getSchool()을 호출하는 경우를 위한 편의 메서드
     * schoolInfo 필드를 그대로 반환합니다.
     */
    public SchoolRequest getSchool() {
        return this.schoolInfo;
    }
}