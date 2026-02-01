package com.nutriassistant.nutriassistant_back.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ERD: users_student
 * - school_id, username, password_hash, name, phone, grade, class_no
 *
 * ✅ 요청 JSON에서 snake_case로 와도 되고(canonical),
 *    프론트가 camelCase로 보내도 받게끔(JsonAlias) 처리함.
 */
public class SignUpRequest {

    @NotNull
    @JsonProperty("school_id")
    @JsonAlias({"schoolId"})
    private Long schoolId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("username")
    @JsonAlias({"email"}) // 혹시 프론트가 email로 보내도 받게
    private String username;

    @NotBlank
    @Size(min = 8, max = 72)
    @JsonProperty("pw")
    @JsonAlias({"password"})
    private String pw;

    @NotBlank
    @Size(max = 100)
    @JsonProperty("name")
    private String name;

    @NotBlank
    @Size(max = 50)
    @JsonProperty("phone")
    private String phone;

    @NotNull
    @JsonProperty("grade")
    private Integer grade;

    @NotNull
    @JsonProperty("class_no")
    @JsonAlias({"classNo", "classroom"})
    private Integer classNo;

    public SignUpRequest() {}

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPw() { return pw; }
    public void setPw(String pw) { this.pw = pw; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public Integer getClassNo() { return classNo; }
    public void setClassNo(Integer classNo) { this.classNo = classNo; }
}
