package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;

import java.time.LocalDateTime;

/**
 * 회원가입 응답 DTO
 *
 * 역할:
 * - 학생 및 영양사 회원가입 성공 시 클라이언트에게 반환할 데이터 구조입니다.
 */
public class SignUpResponse {

    private Long userId;

    @JsonProperty("school_id")
    private Long schoolId;

    private String username;
    private String name;
    private String phone;
    private Integer grade;

    @JsonProperty("class_no")
    private Integer classNo;

    @JsonProperty("allergy_codes")
    private String allergyCodes;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public SignUpResponse() {}

    // [학생용] 전체 필드를 포함하는 생성자
    public SignUpResponse(Student student) {
        this.userId = student.getId();
        this.schoolId = student.getSchoolId();
        this.username = student.getUsername();
        this.name = student.getName();
        this.phone = student.getPhone();
        this.grade = student.getGrade();
        this.classNo = student.getClassNo();
        this.allergyCodes = student.getAllergyCodes();
        this.createdAt = student.getCreatedAt();
        this.updatedAt = student.getUpdatedAt();
    }

    // [영양사용] 필수 식별 정보만 포함하는 생성자 (수정됨)
    // AuthService.signupDietitian 메서드에서 사용합니다.
    public SignUpResponse(Long id, String name) {
        this.userId = id;
        this.name = name;
    }

    // Getters & Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public Integer getClassNo() { return classNo; }
    public void setClassNo(Integer classNo) { this.classNo = classNo; }

    public String getAllergyCodes() { return allergyCodes; }
    public void setAllergyCodes(String allergyCodes) { this.allergyCodes = allergyCodes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}