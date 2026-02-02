package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_ops_docs")
@Getter // 이 어노테이션 덕분에 getReportData()가 자동으로 생성됩니다.
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

    // [수정 핵심] 필드명을 reportContent -> reportData로 변경
    // 이유: Service 코드에서 getReportData()를 호출하고 있기 때문에 이름을 맞춰야 합니다.
    // 타입: MySQL에서 긴 JSON 문자열을 담기 위해 LONGTEXT 사용 (또는 json 타입)
    @Column(name = "report_data", columnDefinition = "LONGTEXT")
    private String reportData;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        // createdAt은 @CreationTimestamp가 처리하지만, 안전장치로 유지해도 무방함
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ReportStatus.PROCESSING;
        }
    }

    // [수정] 메서드 이름도 필드명 변경에 맞춰 수정
    public void updateReportData(String jsonContent) {
        this.reportData = jsonContent;
    }

    public void complete() {
        this.status = ReportStatus.COMPLETED;
    }

    // [삭제] public String getReportData() {}
    // 이유: 클래스 위에 붙은 @Getter 롬복 어노테이션이
    // 자동으로 public String getReportData()를 생성해주므로 직접 작성할 필요가 없습니다.
}