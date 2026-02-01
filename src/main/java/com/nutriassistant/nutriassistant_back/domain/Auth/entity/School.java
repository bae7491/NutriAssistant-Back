package com.nutriassistant.nutriassistant_back.domain.Auth.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "school")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // BIGINT
    private Long id;

    // ERD: dietitian_id (FK) - 학교 소유자 변경 방지(updatable=false)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dietitian_id", nullable = false, unique = true, updatable = false)
    private Dietitian dietitian;

    @Column(name = "school_name", nullable = false, length = 100)
    private String schoolName;

    @Column(name = "school_type", nullable = false, length = 20)
    private String schoolType; // 초/중/고

    @Column(name = "phone", nullable = false, length = 50)
    private String phone;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "student_count", nullable = false)
    private Integer studentCount;

    @Column(name = "target_unit_price", nullable = false)
    private Integer targetUnitPrice;

    @Column(name = "max_unit_price", nullable = false)
    private Integer maxUnitPrice;

    @Lob
    @Column(name = "operation_rules")
    private String operationRules;

    @Column(name = "cook_workers", nullable = false)
    private Integer cookWorkers;

    @Lob
    @Column(name = "kitchen_equipment")
    private String kitchenEquipment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public School() {}

    public Long getId() { return id; }

    public Dietitian getDietitian() { return dietitian; }
    public void setDietitian(Dietitian dietitian) { this.dietitian = dietitian; }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public String getSchoolType() { return schoolType; }
    public void setSchoolType(String schoolType) { this.schoolType = schoolType; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }

    public Integer getTargetUnitPrice() { return targetUnitPrice; }
    public void setTargetUnitPrice(Integer targetUnitPrice) { this.targetUnitPrice = targetUnitPrice; }

    public Integer getMaxUnitPrice() { return maxUnitPrice; }
    public void setMaxUnitPrice(Integer maxUnitPrice) { this.maxUnitPrice = maxUnitPrice; }

    public String getOperationRules() { return operationRules; }
    public void setOperationRules(String operationRules) { this.operationRules = operationRules; }

    public Integer getCookWorkers() { return cookWorkers; }
    public void setCookWorkers(Integer cookWorkers) { this.cookWorkers = cookWorkers; }

    public String getKitchenEquipment() { return kitchenEquipment; }
    public void setKitchenEquipment(String kitchenEquipment) { this.kitchenEquipment = kitchenEquipment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
