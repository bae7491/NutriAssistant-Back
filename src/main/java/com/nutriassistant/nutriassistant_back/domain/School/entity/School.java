package com.nutriassistant.nutriassistant_back.domain.School.entity;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "schools")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "school_id")
    private Long id;

    // [수정] Nutritionist -> Dietitian으로 변경
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dietitian_id")
    private Dietitian dietitian;

    // 나이스 API 필수 정보
    @Column(nullable = false)
    private String schoolName;

    @Column(nullable = false)
    private String regionCode;

    @Column(nullable = false, unique = true)
    private String schoolCode;

    private String address;

    // 학교 상세 정보
    private String schoolType;
    private String phone;
    private String email;
    private Integer studentCount;
    private Integer targetUnitPrice;
    private Integer maxUnitPrice;

    @Column(columnDefinition = "TEXT")
    private String operationRules;

    private Integer cookWorkers;

    @Column(columnDefinition = "TEXT")
    private String kitchenEquipment;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}