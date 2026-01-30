package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FileAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // related_type: "OPS" or "BOARD"
    @Column(name = "related_type", nullable = false)
    private String relatedType;

    // related_id: MonthlyOpsDoc ID or Board ID
    @Column(name = "related_id", nullable = false)
    private Long relatedId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "s3_path", nullable = false)
    private String s3Path;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}