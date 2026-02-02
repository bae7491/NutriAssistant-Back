package com.nutriassistant.nutriassistant_back.domain.reviewanalysis.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_analysis")
@Getter // 이 어노테이션이 getPositiveCount(), getNegativeCount() 등을 자동으로 만들어줍니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReviewAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "target_ym", length = 10)
    private String targetYm;

    @Column(name = "sentiment_label", length = 20)
    private String sentimentLabel;

    @Column(name = "sentiment_score")
    private Float sentimentScore;

    @Column(name = "sentiment_conf")
    private Float sentimentConf;

    // [수정 1] 긍정 리뷰 개수 컬럼 추가
    // DB에 positive_count 컬럼이 있어야 합니다.
    @Column(name = "positive_count")
    private Integer positiveCount;

    // [수정 2] 부정 리뷰 개수 컬럼 추가
    // DB에 negative_count 컬럼이 있어야 합니다.
    @Column(name = "negative_count")
    private Integer negativeCount;

    @Column(name = "aspect_tags", columnDefinition = "TEXT")
    private String aspectTags;

    @Column(name = "evidence_phrases", columnDefinition = "TEXT")
    private String evidencePhrases;

    @Column(name = "issue_flags")
    private Boolean issueFlags;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // [삭제] public Integer getPositiveCount() {}
    // [삭제] public Integer getNegativeCount() {}
    // 이유: 클래스 위에 붙은 @Getter 어노테이션이 이 메서드들을 자동으로 생성해주므로,
    // 직접 작성하면 중복이거나 에러가 날 수 있어 제거했습니다.
}