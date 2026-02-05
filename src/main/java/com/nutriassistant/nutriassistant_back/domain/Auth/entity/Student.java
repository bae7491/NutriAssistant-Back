package com.nutriassistant.nutriassistant_back.domain.Auth.entity;

import com.nutriassistant.nutriassistant_back.domain.Auth.util.PhoneNumberUtil;
import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import com.nutriassistant.nutriassistant_back.global.enums.UserStatus; // ★ Enum 임포트 필수
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users_student")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // [수정 포인트 1] updatable = false 제거 완료 (전학 시 학교 변경 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(name = "username", nullable = false, length = 255, updatable = false)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", nullable = false, length = 50)
    private String phone;

    @Column(name = "grade", nullable = false)
    private Integer grade;

    @Column(name = "class_no", nullable = false)
    private Integer classNo;

    @Column(name = "allergy_codes", length = 100)
    private String allergyCodes;

    // [수정 포인트 2] 회원 상태 필드 추가 (탈퇴/휴면 관리용)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE; // 기본값: 활동 중

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Student() {}

    // =======================================================
    // 비즈니스 로직 메서드
    // =======================================================

    // [추가] 회원 탈퇴 처리 (상태만 변경)
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }

    // =======================================================
    // Getters & Setters
    // =======================================================
    public Long getId() { return id; }

    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

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

    // [추가] 상태 Getter/Setter
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // 전화번호 정규화
    @PrePersist
    @PreUpdate
    private void normalizePhone() {
        if (this.phone != null) {
            this.phone = PhoneNumberUtil.normalizeToDigits(this.phone, PhoneNumberUtil.Mode.MOBILE_ONLY);
        }
    }

    // 학교 ID 반환 편의 메서드
    public Long getSchoolId() {
        return (this.school != null) ? this.school.getId() : null;
    }
}