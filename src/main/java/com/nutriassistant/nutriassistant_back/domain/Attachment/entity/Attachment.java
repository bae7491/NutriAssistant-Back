package com.nutriassistant.nutriassistant_back.domain.Attachment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "attachment")
@Getter
@Setter
@NoArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_type", nullable = false, length = 20)
    private RelatedType relatedType;

    @Column(name = "related_id", nullable = false)
    private Long relatedId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "s3_path", nullable = false, length = 512)
    private String s3Path;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Attachment(RelatedType relatedType, Long relatedId, String fileName, String s3Path, String fileType) {
        this.relatedType = relatedType;
        this.relatedId = relatedId;
        this.fileName = fileName;
        this.s3Path = s3Path;
        this.fileType = fileType;
    }

    public Attachment(RelatedType relatedType, Long relatedId, String fileName, String s3Path, String fileType, Long fileSize) {
        this.relatedType = relatedType;
        this.relatedId = relatedId;
        this.fileName = fileName;
        this.s3Path = s3Path;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }
}
