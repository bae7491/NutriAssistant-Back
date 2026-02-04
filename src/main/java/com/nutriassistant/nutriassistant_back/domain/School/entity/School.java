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
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dietitian_id", nullable = false)
    private Dietitian dietitian;

    // =======================================================
    // 1. 기본 정보 (나이스 API 기반)
    // =======================================================
    // 나중에 학교 이름이나 주소가 바뀔 수도 있으므로 @Setter를 열어두거나,
    // updateBasicInfo 같은 메서드를 만드는 것이 좋습니다.
    // 여기서는 유연하게 @Setter를 추가했습니다.

    @Setter
    @Column(nullable = false)
    private String schoolName;   // 학교명

    @Column(nullable = false, unique = true)
    private String schoolCode;   // 표준학교코드 (이건 절대 바뀌면 안 됨 -> Setter 없음)

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

    // =======================================================
    // 4. 편의 메서드
    // =======================================================

    // [수정] 빈 껍데기 메서드 수정 -> 실제 값을 대입하도록 변경
    public void setDietitian(Dietitian dietitian) {
        this.dietitian = dietitian;
    }
}