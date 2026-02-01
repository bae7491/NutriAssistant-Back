package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;

import java.time.LocalDateTime;

public class SignUpResponse {

    /**
     * ✅ 기존 응답 형태 호환:
     * { "userId": 1 }
     */
    private Long userId;

    // ✅ ERD 필드도 함께 내려주기(프론트/검증/디버깅에 유용)
    @JsonProperty("school_id")
    private Long schoolId;
    private String username;
    private String name;
    private String phone;
    private Integer grade;

    @JsonProperty("class_no")
    private Integer classNo;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public SignUpResponse() {}

    public SignUpResponse(Student student) {
        this.userId = student.getId();
        this.schoolId = student.getSchoolId();
        this.username = student.getUsername();
        this.name = student.getName();
        this.phone = student.getPhone();
        this.grade = student.getGrade();
        this.classNo = student.getClassNo();
        this.createdAt = student.getCreatedAt();
        this.updatedAt = student.getUpdatedAt();
    }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
