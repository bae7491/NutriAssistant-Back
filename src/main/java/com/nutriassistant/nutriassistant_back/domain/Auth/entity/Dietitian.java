package com.nutriassistant.nutriassistant_back.domain.Auth.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.nutriassistant.nutriassistant_back.domain.Auth.util.PhoneNumberUtil;


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
    @Column(name = "id") // BIGINT
    private Long id;

    // ✅ 로그인 식별자: 업데이트 불가 권장
    @Column(name = "username", nullable = false, length = 255, updatable = false) // VARCHAR(255)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255) // VARCHAR(255)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 100) // VARCHAR(100)
    private String name;

    @Column(name = "phone", nullable = false, length = 50) // VARCHAR(50)
    private String phone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false) // DATETIME
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false) // DATETIME
    private LocalDateTime updatedAt;

    public Dietitian() {}

    public Long getId() { return id; }

    public String getUsername() { return username; }
    // 회원가입 시에만 세팅되는 값 (업데이트는 DB 반영 안 됨)
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @PrePersist
    @PreUpdate
    private void normalizePhone() {
        this.phone = PhoneNumberUtil.normalizeToDigits(this.phone, PhoneNumberUtil.Mode.MOBILE_ONLY);
    }

}
