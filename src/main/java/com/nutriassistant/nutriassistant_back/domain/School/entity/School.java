package com.nutriassistant.nutriassistant_back.domain.School.entity;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "school")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "school_id")
    private Long id;

    // 영양사(Dietitian)와 1:1 관계
    // (영양사 탈퇴 시 null 허용을 위해 nullable = true)
    @Setter // ★ 영양사 매칭(registerSchool)을 위해 필요
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dietitian_id", nullable = true)
    private Dietitian dietitian;

    // =======================================================
    // 1. 기본 정보 (나이스 API 기반 + 수정 가능)
    // =======================================================

    @Setter
    @Column(nullable = false)
    private String schoolName;   // 학교명

    @Setter // ★ 정보 업데이트(updateSchoolInfo)를 위해 필요
    @Column(nullable = false, unique = true)
    private String schoolCode;   // 표준학교코드

    @Setter // ★ 정보 업데이트를 위해 필요
    @Column(nullable = false)
    private String regionCode;   // 시도교육청코드

    @Setter
    private String address;      // 주소

    @Setter
    private String schoolType;   // 학교 종류 (초/중/고)

    // =======================================================
    // 2. 운영 정보 (영양사 입력 정보)
    // =======================================================

    @Setter
    private String phone;        // 전화번호

    @Setter
    private String email;        // 이메일

    @Setter
    private Integer studentCount; // 급식 학생 수

    @Setter
    private Integer targetUnitPrice; // 목표 단가

    @Setter
    private Integer maxUnitPrice;    // 최대 단가

    @Setter
    @Column(columnDefinition = "TEXT")
    private String operationRules;   // 운영 규칙

    @Setter
    private Integer cookWorkers;     // 조리 종사자 수

    @Setter
    @Column(columnDefinition = "TEXT")
    private String kitchenEquipment; // 주방 기구 현황

    // =======================================================
    // 3. 타임스탬프
    // =======================================================
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}