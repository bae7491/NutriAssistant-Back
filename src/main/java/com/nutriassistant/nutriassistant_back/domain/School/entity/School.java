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

    // 영양사(Dietitian)와 1:1 관계 (한 학교에 한 명의 관리 영양사)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dietitian_id", nullable = false)
    private Dietitian dietitian;

    // =======================================================
    // 1. 기본 정보 (나이스 API에서 가져오는 불변 정보)
    // =======================================================
    @Column(nullable = false)
    private String schoolName;   // 학교명

    @Column(nullable = false, unique = true) // 학교 코드는 유니크해야 함
    private String schoolCode;   // 표준학교코드 (중복 방지용 key)

    @Column(nullable = false)
    private String regionCode;   // 시도교육청코드

    private String address;      // 주소
    private String schoolType;   // 학교 종류 (초/중/고)

    // =======================================================
    // 2. 운영 정보 (영양사가 직접 입력/수정하는 정보)
    // =======================================================
    // 수정 가능한 필드에만 @Setter를 붙여줍니다.

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
    private String operationRules;   // 운영 규칙 (긴 글)

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