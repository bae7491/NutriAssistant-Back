package com.nutriassistant.nutriassistant_back.domain.Auth.entity;

import com.nutriassistant.nutriassistant_back.domain.Auth.util.PhoneNumberUtil;
import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
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

    // [수정] 학교 정보 매핑
    // FetchType.LAZY: 학생을 조회할 때 학교 데이터까지 즉시 가져오지 않고,
    // getSchool()을 호출하여 실제로 데이터를 사용할 때 쿼리를 날립니다. (성능 최적화)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, updatable = false) // DB 컬럼명 school_id와 매핑
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Student() {}

    // Getters & Setters

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getAllergyCodes() { return allergyCodes; }
    public void setAllergyCodes(String allergyCodes) { this.allergyCodes = allergyCodes; }

    // 전화번호 정규화 (DB 저장/업데이트 전 자동 실행)
    @PrePersist
    @PreUpdate
    private void normalizePhone() {
        if (this.phone != null) {
            this.phone = PhoneNumberUtil.normalizeToDigits(this.phone, PhoneNumberUtil.Mode.MOBILE_ONLY);
        }
    }

    // [수정] 학교 ID 반환 편의 메서드
    // School 객체가 null일 경우 안전하게 null을 반환하도록 처리
    public Long getSchoolId() {
        if (this.school != null) {
            return this.school.getId();
        }
        return null;
    }
}