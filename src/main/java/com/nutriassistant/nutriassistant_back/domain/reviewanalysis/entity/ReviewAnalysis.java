package com.nutriassistant.nutriassistant_back.domain.reviewanalysis.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_analysis")
@Getter
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

    // 긍정 리뷰 개수
    @Column(name = "positive_count")
    private Integer positiveCount;

    // 부정 리뷰 개수
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
}