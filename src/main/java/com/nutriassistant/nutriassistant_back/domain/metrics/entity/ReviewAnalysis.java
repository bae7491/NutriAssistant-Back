package com.nutriassistant.nutriassistant_back.domain.metrics.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "target_ym")
    private String targetYm;

    @Column(name = "sentiment_label")
    private String sentimentLabel; // POSITIVE, NEGATIVE

    @Column(name = "sentiment_score")
    private Float sentimentScore;

    @Column(name = "sentiment_conf")
    private Float sentimentConf;

    // 실제 구현시 JSON Converter 필요. 여기서는 String으로 가정
    @Column(name = "aspect_tags", columnDefinition = "TEXT")
    private String aspectTags;

    @Column(name = "evidence_phrases", columnDefinition = "TEXT")
    private String evidencePhrases;

    @Column(name = "issue_flags")
    private Boolean issueFlags;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}