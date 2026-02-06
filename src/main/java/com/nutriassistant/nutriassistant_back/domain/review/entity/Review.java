package com.nutriassistant.nutriassistant_back.domain.review.entity;

// [▼ 중요] 식단 도메인의 MealType Enum을 임포트해야 합니다.
import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.MealType;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(nullable = false)
    private LocalDate date; // 식수 날짜

    // [▼ 수정] String -> MealType으로 변경
    // DB에는 "LUNCH", "DINNER" 문자열로 저장되도록 설정
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    @Column(nullable = false)
    private Integer rating; // 별점 (1~5)

    @Column(columnDefinition = "TEXT")
    private String content; // 리뷰 내용

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}