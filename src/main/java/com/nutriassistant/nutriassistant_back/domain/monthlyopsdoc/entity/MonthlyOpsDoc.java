package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_ops_docs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MonthlyOpsDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    // [추가] 리포트의 상세 데이터(통계, 텍스트 등)를 JSON으로 저장
    // MySQL 5.7+ 또는 PostgreSQL 사용 시 JSON 타입으로 매핑됨
    @Column(name = "report_content", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private String reportContent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ReportStatus.PROCESSING;
        }
    }

    // 리포트 내용 업데이트를 위한 메서드
    public void updateReportContent(String jsonContent) {
        this.reportContent = jsonContent;
    }

    public void complete() {
        this.status = ReportStatus.COMPLETED;
    }
}