package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DietitianSignUpRequest {

    @NotBlank
    private String username;

    @NotBlank
    @Size(min = 8, max = 72)
    @JsonProperty("pw")
    private String pw;

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    // ✅ 영양사 회원가입하면서 학교정보도 같이 기재
    @NotNull
    @Valid
    private SchoolRequest school;

    public DietitianSignUpRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPw() { return pw; }
    public void setPw(String pw) { this.pw = pw; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public SchoolRequest getSchool() { return school; }
    public void setSchool(SchoolRequest school) { this.school = school; }
}
