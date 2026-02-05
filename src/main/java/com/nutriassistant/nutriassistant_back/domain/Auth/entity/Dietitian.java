package com.nutriassistant.nutriassistant_back.domain.Auth.entity;

import com.nutriassistant.nutriassistant_back.domain.Auth.util.PhoneNumberUtil;
import com.nutriassistant.nutriassistant_back.global.enums.UserStatus; // UserStatus Enum 임포트
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users_dietitian",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_dietitian_username", columnNames = "username")
        }
)
public class Dietitian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, length = 255, updatable = false)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", nullable = false, length = 50)
    private String phone;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    // ▼▼▼ [추가] 회원 상태 관리 필드 ▼▼▼
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Dietitian() {}

    // ▼▼▼ [추가] 탈퇴 비즈니스 로직 ▼▼▼
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }

    // Getters & Setters
    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getPw() { return this.passwordHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @PrePersist
    @PreUpdate
    private void normalizePhone() {
        if (this.phone != null) {
            this.phone = PhoneNumberUtil.normalizeToDigits(this.phone, PhoneNumberUtil.Mode.MOBILE_ONLY);
        }
    }
}