// [파일 위치] src/main/java/com/nutriassistant/nutriassistant_back/entity/MenuHistory.java

package com.nutriassistant.nutriassistant_back.domain.MealPlan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "menu_history_log") // 테이블 이름 지정
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class) // 생성 시간 자동 기록용
public class MenuHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(nullable = false)
    private String mealDate; // 예: "2026-03-03"

    @Column(nullable = false)
    private String mealType; // 예: "중식"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType; // 변경 유형 (AI대체 / 수동수정)

    @Column(columnDefinition = "TEXT") // 긴 문자열 저장
    private String oldMenus; // 변경 전 메뉴 리스트 (문자열로 변환하여 저장)

    @Column(columnDefinition = "TEXT")
    private String newMenus; // 변경 후 메뉴 리스트

    @Column(length = 1000)
    private String reason; // 변경 사유 또는 AI 코멘트

    // 식단표 원본 생성 시간 (MealPlanMenu의 createdAt)
    @Column(name = "menu_created_at")
    private LocalDateTime menuCreatedAt;

    // 히스토리 레코드 생성 시간 = 수정 발생 시간
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 변경 유형 정의 Enum
    public enum ActionType {
        AI_AUTO_REPLACE, // AI 원클릭 대체
        MANUAL_UPDATE    // 수동 수정
    }
}